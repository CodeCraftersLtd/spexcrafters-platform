/**
 * Replace `{name}`-style placeholders in a dictionary message.
 *
 * Lives in its own module (re-exported from ./i18n) so Client Components can
 * import it without pulling every locale dictionary into the browser bundle.
 */
export function interpolate(
  template: string,
  values: Record<string, string | number>,
): string {
  return template.replace(/\{(\w+)\}/g, (match, key: string) => {
    const value = values[key];
    return value === undefined ? match : String(value);
  });
}
