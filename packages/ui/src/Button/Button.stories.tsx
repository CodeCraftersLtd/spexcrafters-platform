import type { Meta, StoryObj } from "@storybook/react";
import { Button } from "./Button";

/**
 * §5 state coverage: default (each variant), focus-visible (play), disabled,
 * loading, plus dark theme and long-content stories (§8). Hover/active are
 * one-ramp-step token shifts exercised via real pointer interaction — see
 * Button.module.css.
 */
const meta = {
  title: "Primitives/Button",
  component: Button,
  args: {
    children: "Request quotation",
  },
} satisfies Meta<typeof Button>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Primary: Story = {
  args: { variant: "primary" },
};

export const Secondary: Story = {
  args: { variant: "secondary" },
};

export const Quiet: Story = {
  args: { variant: "quiet" },
};

export const Destructive: Story = {
  args: { variant: "destructive", children: "Withdraw RFQ" },
};

export const SmallSize: Story = {
  args: { size: "sm" },
};

export const FocusVisible: Story = {
  play: async ({ canvasElement }) => {
    canvasElement.querySelector("button")?.focus();
  },
};

export const Disabled: Story = {
  args: { disabled: true },
};

export const DisabledSecondary: Story = {
  args: { variant: "secondary", disabled: true },
};

/** Width is preserved: the label keeps its space; spinner overlays it. */
export const Loading: Story = {
  args: { loading: true },
};

export const LoadingSecondary: Story = {
  args: { variant: "secondary", loading: true },
};

/** German strings + 40-character SKU code (§8 long-content rule). */
export const LongContent: Story = {
  args: {
    children:
      "Unverbindliche Großhandelspreisanfrage senden — SKU CR39-SPH-MINUS-0800-PLUS-0600-AR-HC-UV",
  },
};

export const DarkTheme: Story = {
  globals: { theme: "dark" },
};

export const DarkThemeLoading: Story = {
  args: { loading: true },
  globals: { theme: "dark" },
};
