import type { Meta, StoryObj } from "@storybook/react";
import { VisuallyHidden } from "./VisuallyHidden";

/**
 * AT-only text utility — the standard clip pattern (never display: none).
 * Only "Visible text" renders on screen; screen readers announce both.
 */
const meta = {
  title: "Primitives/VisuallyHidden",
  component: VisuallyHidden,
} satisfies Meta<typeof VisuallyHidden>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: () => (
    <p>
      Visible text
      <VisuallyHidden> — and this suffix is announced but not shown</VisuallyHidden>
    </p>
  ),
};

/** Typical usage: giving a terse visual control an explicit accessible name. */
export const AccessibleName: Story = {
  render: () => (
    <button type="button">
      ×<VisuallyHidden>Remove filter: CR-39</VisuallyHidden>
    </button>
  ),
};
