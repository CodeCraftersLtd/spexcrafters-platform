import type { Meta, StoryObj } from "@storybook/react";
import { Spinner } from "./Spinner";

/**
 * Action-scale busy indicator: role="status" + visually hidden label.
 * The rotation is removed (not shortened) under prefers-reduced-motion.
 */
const meta = {
  title: "Primitives/Spinner",
  component: Spinner,
} satisfies Meta<typeof Spinner>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Small: Story = {
  args: { size: "sm" },
};

/** The label is announced by screen readers ("Loading quotes…"). */
export const CustomLabel: Story = {
  args: { label: "Loading quotes…" },
};

export const DarkTheme: Story = {
  globals: { theme: "dark" },
};
