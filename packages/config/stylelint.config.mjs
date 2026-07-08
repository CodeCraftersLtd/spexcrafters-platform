/**
 * SpexCrafters Stylelint preset.
 *
 * Enforces the design-system rules from docs/design-system/design-system.md:
 * - tokens-only styling (no raw hex colors in component CSS)
 * - no arbitrary z-index values (use --sc-z-* tokens)
 * - the restricted spectral-gradient token may only appear in whitelisted files
 */
export default {
  extends: ['stylelint-config-standard'],
  rules: {
    'color-no-hex': true,
    'declaration-property-value-disallowed-list': {
      'z-index': [/^(?!var\(--sc-z-).*$/],
    },
    'custom-property-pattern': [
      '^sc-[a-z0-9-]+$',
      { message: 'Custom properties must use the --sc-* namespace' },
    ],
  },
  overrides: [
    {
      // Only the tokens package may define primitives / raw values.
      files: ['packages/design-tokens/**/*.css'],
      rules: { 'color-no-hex': null },
    },
  ],
};
