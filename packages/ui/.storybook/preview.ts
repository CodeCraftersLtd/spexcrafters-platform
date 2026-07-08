import type { Decorator, Preview } from "@storybook/react";

import "@spexcrafters/design-tokens/css";

/**
 * Theme toggling mirrors the app mechanism (design-system.md §1.4):
 * `data-theme` on the document element + matching `color-scheme` (set by the
 * token stylesheet). Component CSS never branches on theme.
 */
const withTheme: Decorator = (Story, context) => {
  const theme = context.globals["theme"] === "dark" ? "dark" : "light";
  document.documentElement.dataset["theme"] = theme;
  document.body.style.backgroundColor = "var(--sc-color-bg-page)";
  document.body.style.color = "var(--sc-color-text-primary)";
  return Story();
};

const preview: Preview = {
  decorators: [withTheme],
  globalTypes: {
    theme: {
      description: "Design-token theme (data-theme attribute)",
      toolbar: {
        title: "Theme",
        icon: "mirror",
        items: ["light", "dark"],
        dynamicTitle: true,
      },
    },
  },
  initialGlobals: {
    theme: "light",
  },
  parameters: {
    // Theme backgrounds come from the token stylesheet, not the addon.
    backgrounds: { disable: true },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
};

export default preview;
