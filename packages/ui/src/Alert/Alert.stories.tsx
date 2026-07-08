import type { Meta, StoryObj } from "@storybook/react";
import { Alert } from "./Alert";

/**
 * Icon-free v1: tone = border + bg + title/body text (AA pairs, §2.5).
 * info/success → role="status"; warning/danger → role="alert".
 */
const meta = {
  title: "Primitives/Alert",
  component: Alert,
  args: {
    tone: "info",
    title: "Prices shown in EUR",
    children: "Estimates only — orders are invoiced in USD.",
  },
} satisfies Meta<typeof Alert>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Info: Story = {};

export const Success: Story = {
  args: {
    tone: "success",
    title: "RFQ sent",
    children: "Your request reached 6 verified suppliers. Expect first quotes within 48 hours.",
  },
};

export const Warning: Story = {
  args: {
    tone: "warning",
    title: "Certificate expiring",
    children: "The CE certificate for CR39-SPH lenses expires on 2026-08-01. Upload a renewal to keep the listing active.",
  },
};

export const Danger: Story = {
  args: {
    tone: "danger",
    title: "Quotation failed",
    children: "The supplier could not be reached. Your RFQ is unaffected — retry sending the quotation request.",
  },
};

export const WithoutTitle: Story = {
  render: () => (
    <Alert tone="info">
      Prices shown in EUR are estimates; orders are invoiced in USD.
    </Alert>
  ),
};

/** German strings + 40-character SKU code (§8 long-content rule). */
export const LongContent: Story = {
  args: {
    tone: "warning",
    title: "Lieferzeitänderung für laufende Bestellungen",
    children:
      "Die voraussichtliche Produktions- und Versandzeit für den Artikel CR39-SPH-MINUS-0800-PLUS-0600-AR-HC-UV42 hat sich aufgrund von Kapazitätsengpässen beim Beschichtungspartner von 12–15 Tagen auf 18–22 Tage verlängert. Bereits bestätigte Liefertermine bleiben davon unberührt.",
  },
};

export const DarkTheme: Story = {
  args: {
    tone: "success",
    title: "RFQ sent",
    children: "Your request reached 6 verified suppliers.",
  },
  globals: { theme: "dark" },
};

export const DarkThemeDanger: Story = {
  args: {
    tone: "danger",
    title: "Quotation failed",
    children: "The supplier could not be reached.",
  },
  globals: { theme: "dark" },
};
