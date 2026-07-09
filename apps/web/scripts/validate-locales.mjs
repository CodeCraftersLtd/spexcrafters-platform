#!/usr/bin/env node
/**
 * Locale message validator (Phase 7 — Tier-3 structural CI, all 20 locales).
 *
 * Zero-dependency. Fails (exit 1) on any of:
 *   - a registry locale missing its message directory, or an unexpected dir
 *   - a missing or unexpected namespace file
 *   - invalid JSON, or a duplicate key within any object
 *   - key parity drift vs the `en` source (missing OR extra keys)
 *   - invalid ICU (unbalanced braces; a plural/select without an `other` branch)
 *   - placeholder / plural argument drift vs `en` for the same key
 *   - RTL-registry inconsistency (declared RTL set must be a subset of locales)
 *
 * The registry constants below are duplicated from src/i18n/locales.ts and
 * src/i18n/messages.ts by design: the validator must stay dependency-free and
 * runnable in CI without a TypeScript toolchain. Keep them in sync (the parity
 * checks here would catch a drift in the message set regardless).
 */
import { readdirSync, readFileSync, statSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const HERE = dirname(fileURLToPath(import.meta.url));
const MESSAGES_DIR = join(HERE, '..', 'messages');

const LOCALES = [
  'en', 'zh-CN', 'ko', 'ja', 'th', 'vi', 'fr', 'es', 'id', 'ms',
  'hi', 'ru', 'bn', 'de', 'ur', 'fil', 'fa', 'pt', 'ar', 'tr',
];
const RTL_LOCALES = ['ar', 'fa', 'ur'];
const SOURCE_LOCALE = 'en';
const NAMESPACES = [
  'common', 'navigation', 'auth', 'organizations', 'suppliers', 'verification',
  'evidence', 'reviewer', 'errors', 'accessibility', 'seo', 'taxonomy',
  'taxonomyAdmin',
];

const errors = [];
const fail = (msg) => errors.push(msg);

/** ---- Duplicate-key aware JSON scan (validity + duplicate detection) ---- */
function detectDuplicateKeys(text, label) {
  let i = 0;
  const n = text.length;
  const stack = []; // frames: { type, keys:Set }
  const dups = [];

  function skipWs() {
    while (i < n && /\s/.test(text[i])) i++;
  }
  function readString() {
    // assumes text[i] === '"'
    let s = '';
    i++;
    while (i < n) {
      const c = text[i];
      if (c === '\\') {
        s += text[i] + text[i + 1];
        i += 2;
        continue;
      }
      if (c === '"') {
        i++;
        return s;
      }
      s += c;
      i++;
    }
    throw new Error('unterminated string');
  }

  skipWs();
  while (i < n) {
    const c = text[i];
    if (c === '{') {
      stack.push({ type: 'object', keys: new Set(), expectKey: true });
      i++;
      skipWs();
      continue;
    }
    if (c === '[') {
      stack.push({ type: 'array' });
      i++;
      continue;
    }
    if (c === '}' || c === ']') {
      stack.pop();
      i++;
      continue;
    }
    if (c === ',') {
      const top = stack[stack.length - 1];
      if (top && top.type === 'object') top.expectKey = true;
      i++;
      skipWs();
      continue;
    }
    if (c === ':') {
      i++;
      continue;
    }
    if (c === '"') {
      const top = stack[stack.length - 1];
      const isKey = top && top.type === 'object' && top.expectKey;
      const str = readString();
      if (isKey) {
        if (top.keys.has(str)) dups.push(str);
        top.keys.add(str);
        top.expectKey = false;
      }
      continue;
    }
    // numbers / literals / whitespace
    i++;
  }
  if (dups.length > 0) {
    fail(`${label}: duplicate key(s): ${[...new Set(dups)].join(', ')}`);
  }
}

/** ---- Flatten to dot-path leaves ---- */
function flatten(obj, prefix, out) {
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      flatten(value, path, out);
    } else {
      out.set(path, value);
    }
  }
  return out;
}

/** ---- ICU checks ---- */
const COMPLEX_RE = /\{\s*([A-Za-z0-9_]+)\s*,\s*(plural|selectordinal|select|number|date|time)\b/g;
const SIMPLE_RE = /\{\s*([A-Za-z0-9_]+)\s*\}/g;

function icuSignature(message, label) {
  // Balance check.
  let depth = 0;
  for (const ch of message) {
    if (ch === '{') depth++;
    else if (ch === '}') {
      depth--;
      if (depth < 0) break;
    }
  }
  if (depth !== 0) fail(`${label}: unbalanced ICU braces in "${message}"`);

  const complex = new Map();
  let m;
  COMPLEX_RE.lastIndex = 0;
  while ((m = COMPLEX_RE.exec(message)) !== null) {
    complex.set(m[1], m[2]);
    if ((m[2] === 'plural' || m[2] === 'select' || m[2] === 'selectordinal')) {
      // require an `other` branch after this argument.
      const rest = message.slice(m.index);
      if (!/\bother\s*\{/.test(rest)) {
        fail(`${label}: ${m[2]} argument "${m[1]}" is missing an "other" branch in "${message}"`);
      }
    }
  }

  const simple = new Set();
  SIMPLE_RE.lastIndex = 0;
  while ((m = SIMPLE_RE.exec(message)) !== null) {
    if (!complex.has(m[1])) simple.add(m[1]);
  }

  const parts = [];
  for (const name of [...simple].sort()) parts.push(`${name}:simple`);
  for (const name of [...complex.keys()].sort()) parts.push(`${name}:${complex.get(name)}`);
  return parts.join('|');
}

/** ---- Load + validate one namespace file ---- */
function loadNamespace(locale, ns) {
  const file = join(MESSAGES_DIR, locale, `${ns}.json`);
  if (!existsSync(file)) {
    fail(`${locale}/${ns}.json: file is missing`);
    return null;
  }
  const text = readFileSync(file, 'utf8');
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch (e) {
    fail(`${locale}/${ns}.json: invalid JSON — ${e.message}`);
    return null;
  }
  detectDuplicateKeys(text, `${locale}/${ns}.json`);
  return parsed;
}

/** ---- Registry consistency ---- */
for (const rtl of RTL_LOCALES) {
  if (!LOCALES.includes(rtl)) fail(`registry: RTL locale "${rtl}" is not in the locale set`);
}

/** ---- Directory checks ---- */
if (!existsSync(MESSAGES_DIR)) {
  fail(`messages directory not found at ${MESSAGES_DIR}`);
} else {
  const presentDirs = readdirSync(MESSAGES_DIR).filter((entry) => {
    try {
      return statSync(join(MESSAGES_DIR, entry)).isDirectory();
    } catch {
      return false;
    }
  });
  for (const locale of LOCALES) {
    if (!presentDirs.includes(locale)) fail(`missing message directory for locale "${locale}"`);
  }
  for (const dir of presentDirs) {
    if (!LOCALES.includes(dir)) fail(`unexpected/unsupported locale directory "${dir}"`);
  }
}

/** ---- Build the en reference (keys + ICU signatures per namespace) ---- */
const reference = {}; // ns -> Map<path, signature>
for (const ns of NAMESPACES) {
  const data = loadNamespace(SOURCE_LOCALE, ns);
  if (!data) continue;
  const flat = flatten(data, '', new Map());
  const sig = new Map();
  for (const [path, value] of flat) {
    if (typeof value !== 'string') {
      fail(`${SOURCE_LOCALE}/${ns}.json: value at "${path}" is not a string`);
      continue;
    }
    sig.set(path, icuSignature(value, `${SOURCE_LOCALE}/${ns}.json:${path}`));
  }
  reference[ns] = sig;
}

/** ---- Validate every locale against the reference ---- */
for (const locale of LOCALES) {
  for (const ns of NAMESPACES) {
    const refSig = reference[ns];
    if (!refSig) continue;
    const data = loadNamespace(locale, ns);
    if (!data) continue;

    const flat = flatten(data, '', new Map());

    // Extra keys.
    for (const path of flat.keys()) {
      if (!refSig.has(path)) fail(`${locale}/${ns}.json: unexpected key "${path}" (not in en)`);
    }
    // Missing keys + ICU + placeholder/plural parity.
    for (const [path, refSignature] of refSig) {
      if (!flat.has(path)) {
        fail(`${locale}/${ns}.json: missing key "${path}" (present in en)`);
        continue;
      }
      const value = flat.get(path);
      if (typeof value !== 'string') {
        fail(`${locale}/${ns}.json: value at "${path}" is not a string`);
        continue;
      }
      const sig = icuSignature(value, `${locale}/${ns}.json:${path}`);
      if (locale !== SOURCE_LOCALE && sig !== refSignature) {
        fail(
          `${locale}/${ns}.json: placeholder/plural drift at "${path}" — en has [${refSignature || 'none'}], ${locale} has [${sig || 'none'}]`,
        );
      }
    }
  }
}

/** ---- Report ---- */
if (errors.length > 0) {
  console.error(`✗ Locale validation failed with ${errors.length} problem(s):\n`);
  for (const e of errors) console.error(`  - ${e}`);
  process.exit(1);
}
console.log(
  `✓ Locale validation passed: ${LOCALES.length} locales × ${NAMESPACES.length} namespaces, key parity + ICU + placeholder/plural parity OK.`,
);
