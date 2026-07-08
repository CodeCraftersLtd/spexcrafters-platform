import type { Meta, StoryObj } from "@storybook/react";
import { Input } from "../Input/Input";
import { FormField } from "./FormField";

/**
 * FormField clones its element child to inject `aria-describedby`
 * (error id first) and `aria-invalid` automatically — inspect the DOM in
 * these stories to see the wiring.
 */
const meta = {
  title: "Primitives/FormField",
  component: FormField,
  args: {
    label: "Company name",
    htmlFor: "company-name",
    children: <Input id="company-name" placeholder="e.g. Meridian Optics GmbH" />,
  },
} satisfies Meta<typeof FormField>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const WithHint: Story = {
  args: {
    hint: "As registered with your chamber of commerce.",
  },
};

/** Error id gets role="alert"; the child input receives aria-invalid and
 *  aria-describedby="company-name-error company-name-hint". */
export const WithError: Story = {
  args: {
    hint: "As registered with your chamber of commerce.",
    error: "Company name is required.",
  },
};

export const ErrorOnly: Story = {
  args: {
    error: "Company name is required.",
  },
};

/** German strings + long values (§8 long-content rule). */
export const LongContent: Story = {
  args: {
    label:
      "Umsatzsteuer-Identifikationsnummer der Niederlassung (gemäß §27a UStG)",
    htmlFor: "vat-id",
    hint: "Beispiel: DE 123 456 789 — wird für die innergemeinschaftliche Lieferung benötigt und auf allen Rechnungen ausgewiesen.",
    error:
      "Die angegebene Umsatzsteuer-Identifikationsnummer konnte nicht validiert werden. Bitte überprüfen Sie Ihre Eingabe.",
    children: <Input id="vat-id" defaultValue="DE-UST-ID-1234567890-NIEDERLASSUNG-BERLIN" />,
  },
};

export const DarkTheme: Story = {
  args: {
    hint: "As registered with your chamber of commerce.",
    error: "Company name is required.",
  },
  globals: { theme: "dark" },
};
