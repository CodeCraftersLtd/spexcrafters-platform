#!/usr/bin/env node
/**
 * build-tokens.mjs — zero-dependency token build for @spexcrafters/design-tokens.
 *
 * Reads the DTCG-ish JSON sources in src/ and deterministically emits:
 *   build/css/tokens.css  (:root primitives, [data-theme] semantic blocks,
 *                          reduced-motion collapse, focus ring, .sc-annotation)
 *   build/ts/tokens.ts    (typed `as const` objects for non-CSS consumers)
 *
 * No Style Dictionary / no dependencies on purpose: the traversal is a plain
 * depth-first walk in source key order, so the output is stable, reviewable,
 * and reproducible byte-for-byte.
 *
 * Usage:
 *   node scripts/build-tokens.mjs           # write build outputs
 *   node scripts/build-tokens.mjs --check   # regenerate in memory and compare;
 *                                           # exit 1 on drift (CI gate)
 */

import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");

const PRIMITIVE_SOURCES = [
  "color.tokens.json",
  "typography.tokens.json",
  "space.tokens.json",
  "motion.tokens.json",
];

/** Top-level source group → TS export name. */
const TS_EXPORT_NAMES = {
  color: "colors",
  gradient: "gradients",
  font: "fonts",
  text: "text",
  leading: "leading",
  tracking: "tracking",
  space: "space",
  radius: "radius",
  shadow: "shadows",
  border: "borders",
  z: "z",
  grid: "grid",
  bp: "breakpoints",
  motion: "motion",
};

const readJson = (relPath) =>
  JSON.parse(readFileSync(join(ROOT, relPath), "utf8"));

const isToken = (node) =>
  node !== null && typeof node === "object" && "$value" in node;

/** Depth-first flatten in source key order → [[pathParts, $value], ...]. */
function flatten(node, path = [], out = []) {
  if (isToken(node)) {
    out.push([path, node.$value]);
    return out;
  }
  for (const key of Object.keys(node)) {
    if (key.startsWith("$")) continue;
    flatten(node[key], [...path, key], out);
  }
  return out;
}

const cssVar = (parts) => `--sc-${parts.join("-")}`;

/** `{color.accent.600}` → `var(--sc-color-accent-600)` (CSS output). */
const refsToCssVars = (value) =>
  typeof value === "string"
    ? value.replace(/\{([^}]+)\}/g, (_, p) => `var(--sc-${p.split(".").join("-")})`)
    : value;

/** `{color.accent.600}` → raw primitive value (TS output). */
function refsToRaw(value, table) {
  if (typeof value !== "string") return value;
  return value.replace(/\{([^}]+)\}/g, (_, p) => {
    const raw = table.get(p);
    if (raw === undefined) throw new Error(`Unknown token reference: {${p}}`);
    return String(raw);
  });
}

/** Mirror a token tree as plain values, resolving references via `table`. */
function toValueTree(node, table) {
  if (isToken(node)) return refsToRaw(node.$value, table);
  const out = {};
  for (const key of Object.keys(node)) {
    if (key.startsWith("$")) continue;
    out[key] = toValueTree(node[key], table);
  }
  return out;
}

const decls = (tokens, indent) =>
  tokens
    .map(([parts, value]) => `${indent}${cssVar(parts)}: ${refsToCssVars(value)};`)
    .join("\n");

// ---------------------------------------------------------------------------
// Load sources
// ---------------------------------------------------------------------------

const sources = PRIMITIVE_SOURCES.map((f) => readJson(`src/${f}`));
const light = readJson("src/themes/light.json");
const dark = readJson("src/themes/dark.json");

const primitiveTokens = sources.flatMap((json) => flatten(json));
const table = new Map(primitiveTokens.map(([parts, value]) => [parts.join("."), value]));

// ---------------------------------------------------------------------------
// CSS
// ---------------------------------------------------------------------------

const CSS_HEADER = `/**
 * @spexcrafters/design-tokens — GENERATED FILE, do not edit by hand.
 * Source of truth: src/*.tokens.json + src/themes/*.json
 * Regenerate with: pnpm --filter @spexcrafters/design-tokens build
 */
`;

const durationTokens = primitiveTokens.filter(
  ([parts]) => parts[0] === "motion" && parts[1] === "duration",
);

const FOCUS_AND_MIXINS = `/* System-wide focus ring (design-system.md §5): one implementation, applied via
 * :focus-visible (keyboard/AT) — no ring flash on mouse click. Never use
 * outline: none without a replacement. */
:focus-visible,
.sc-focus-visible {
  outline: 2px solid var(--sc-color-focus-ring);
  outline-offset: 2px;
  border-radius: inherit;
}

/* .sc-annotation — the single sanctioned "technical label" treatment
 * (design-system.md §3.3): 2xs mono, uppercase, caps tracking. */
.sc-annotation {
  font-family: var(--sc-font-mono);
  font-size: var(--sc-text-2xs);
  font-weight: var(--sc-font-weight-regular);
  line-height: var(--sc-leading-ui);
  letter-spacing: var(--sc-tracking-caps);
  text-transform: uppercase;
}
`;

const css = [
  CSS_HEADER,
  "/* Tier 1 — primitives (immutable per brand) */",
  ":root {",
  decls(primitiveTokens, "  "),
  "}",
  "",
  "/* Tier 2 — semantic (light, default) */",
  ':root,\n[data-theme="light"] {',
  "  color-scheme: light;",
  decls(flatten(light), "  "),
  "}",
  "",
  "/* Tier 2 — semantic (dark, authenticated dashboards) */",
  '[data-theme="dark"] {',
  "  color-scheme: dark;",
  decls(flatten(dark), "  "),
  "}",
  "",
  "/* Reduced-motion policy (design-system.md §4.6): collapse all durations. */",
  "@media (prefers-reduced-motion: reduce) {",
  "  :root {",
  decls(durationTokens.map(([parts]) => [parts, "0ms"]), "    "),
  "  }",
  "}",
  "",
  FOCUS_AND_MIXINS,
].join("\n");

// ---------------------------------------------------------------------------
// TS
// ---------------------------------------------------------------------------

const TS_HEADER = `/**
 * @spexcrafters/design-tokens — GENERATED FILE, do not edit by hand.
 * Source of truth: src/*.tokens.json + src/themes/*.json
 * Regenerate with: pnpm --filter @spexcrafters/design-tokens build
 *
 * Raw values for non-CSS consumers (charts, <canvas>, OG images, email).
 * Note: fonts.* and borders.* are CSS value strings and may contain var()
 * references — only meaningful where CSS custom properties resolve.
 */
`;

let ts = TS_HEADER + "\n";
for (const json of sources) {
  for (const key of Object.keys(json)) {
    const exportName = TS_EXPORT_NAMES[key];
    if (!exportName) throw new Error(`No TS export name for group "${key}"`);
    ts += `export const ${exportName} = ${JSON.stringify(toValueTree(json[key], table), null, 2)} as const;\n\n`;
  }
}
ts += `export const themes = ${JSON.stringify(
  { light: toValueTree(light, table), dark: toValueTree(dark, table) },
  null,
  2,
)} as const;\n\n`;
ts += "export type ThemeName = keyof typeof themes;\n";

// ---------------------------------------------------------------------------
// Write / check
// ---------------------------------------------------------------------------

const OUTPUTS = [
  ["build/css/tokens.css", css],
  ["build/ts/tokens.ts", ts],
];

const check = process.argv.includes("--check");
let drift = false;

for (const [relPath, content] of OUTPUTS) {
  const absPath = join(ROOT, relPath);
  if (check) {
    let existing = "";
    try {
      existing = readFileSync(absPath, "utf8");
    } catch {
      // missing file counts as drift
    }
    // Normalize CRLF so a Windows checkout with autocrlf does not false-positive.
    if (existing.replace(/\r\n/g, "\n") !== content) {
      console.error(`[tokens] STALE: ${relPath} does not match src/ — run \`pnpm build\``);
      drift = true;
    }
  } else {
    mkdirSync(dirname(absPath), { recursive: true });
    writeFileSync(absPath, content, "utf8");
    console.log(`[tokens] wrote ${relPath}`);
  }
}

if (check) {
  if (drift) process.exit(1);
  console.log("[tokens] build outputs are up to date");
}
