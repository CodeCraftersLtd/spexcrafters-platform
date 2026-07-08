import type { Meta, StoryObj } from "@storybook/react";
import { Input } from "./Input";

/**
 * Inputs are always labelled in real usage — see the FormField stories for
 * the composed pattern. `aria-label` is used here only to keep isolated
 * stories axe-clean.
 */
const meta = {
  title: "Primitives/Input",
  component: Input,
  args: {
    "aria-label": "Company name",
    placeholder: "e.g. Meridian Optics GmbH",
  },
} satisfies Meta<typeof Input>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Filled: Story = {
  args: { defaultValue: "Meridian Optics GmbH" },
};

export const FocusVisible: Story = {
  play: async ({ canvasElement }) => {
    canvasElement.querySelector("input")?.focus();
  },
};

/** Error state: border-error token + aria-invalid. Message wiring is
 *  demonstrated in FormField/WithError. */
export const Invalid: Story = {
  args: { invalid: true, defaultValue: "-13.25" },
};

export const Disabled: Story = {
  args: { disabled: true, defaultValue: "Verified 2026-05" },
};

/** German strings + 40-character SKU code (§8 long-content rule). */
export const LongContent: Story = {
  args: {
    defaultValue:
      "CR39-SPH-MINUS-0800-PLUS-0600-AR-HC-UV42 — Sonderanfertigung für Präzisionsmessgeräte",
  },
};

export const DarkTheme: Story = {
  globals: { theme: "dark" },
};

export const DarkThemeInvalid: Story = {
  args: { invalid: true, defaultValue: "-13.25" },
  globals: { theme: "dark" },
};
