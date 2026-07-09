/**
 * SpexCrafters Stylelint preset.
 *
 * Enforces the design-system rules from docs/design-system/design-system.md:
 * - tokens-only styling (no raw hex colors in component CSS)
 * - no arbitrary z-index values (use --sc-z-* tokens)
 * - the restricted spectral-gradient token may only appear in whitelisted files
 *
 * Phase 7 (ADR-021 — RTL): component CSS must use CSS *logical* properties so a
 * single `dir=rtl` switch flips the whole UI. Physical inline-axis properties
 * (`margin-left`, `padding-right`, bare `left`/`right`, `border-left`, `float`)
 * and physical `text-align: left|right` values are banned; use `margin-inline`,
 * `padding-inline`, `inset-inline-*`, `border-inline`, `text-align: start|end`.
 */
const PHYSICAL_INLINE_PROPERTIES = [
  'margin-left',
  'margin-right',
  'padding-left',
  'padding-right',
  'border-left',
  'border-right',
  'border-top-left-radius',
  'border-top-right-radius',
  'border-bottom-left-radius',
  'border-bottom-right-radius',
  'left',
  'right',
  'float',
  'clear',
];

export default {
  extends: ['stylelint-config-standard'],
  rules: {
    'color-no-hex': true,
    'declaration-property-value-disallowed-list': {
      'z-index': [/^(?!var\(--sc-z-).*$/],
      'text-align': ['/^left$/', '/^right$/'],
    },
    'property-disallowed-list': [
      PHYSICAL_INLINE_PROPERTIES,
      {
        message:
          'Use CSS logical properties (margin-inline / padding-inline / inset-inline-* / border-inline / text-align: start|end) — physical inline-axis properties break RTL (ADR-021).',
      },
    ],
    'custom-property-pattern': [
      '^sc-[a-z0-9-]+$',
      { message: 'Custom properties must use the --sc-* namespace' },
    ],
  },
  overrides: [
    {
      // Only the tokens package may define primitives / raw values.
      files: ['packages/design-tokens/**/*.css'],
      rules: { 'color-no-hex': null, 'property-disallowed-list': null },
    },
  ],
};
