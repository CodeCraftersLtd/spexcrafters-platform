import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Input } from "../Input/Input";
import { FormField } from "./FormField";

describe("FormField", () => {
  it("associates the label with the control", () => {
    render(
      <FormField label="Company name" htmlFor="company">
        <Input id="company" />
      </FormField>,
    );
    const input = screen.getByLabelText("Company name");
    expect(input).toHaveAttribute("id", "company");
  });

  it("wires hint and error ids into the child's aria-describedby (error first)", () => {
    render(
      <FormField
        label="Company name"
        htmlFor="company"
        hint="As registered"
        error="Company name is required."
      >
        <Input id="company" />
      </FormField>,
    );
    const input = screen.getByLabelText("Company name");
    expect(input).toHaveAttribute("aria-describedby", "company-error company-hint");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByText("As registered")).toHaveAttribute("id", "company-hint");
  });

  it("renders the error with role=alert and the derived id", () => {
    render(
      <FormField label="Company name" htmlFor="company" error="Required">
        <Input id="company" />
      </FormField>,
    );
    const error = screen.getByRole("alert");
    expect(error).toHaveAttribute("id", "company-error");
    expect(error).toHaveTextContent("Required");
    expect(screen.getByLabelText("Company name")).toHaveAttribute(
      "aria-describedby",
      "company-error",
    );
  });

  it("adds no aria wiring when there is no hint or error", () => {
    render(
      <FormField label="Company name" htmlFor="company">
        <Input id="company" />
      </FormField>,
    );
    const input = screen.getByLabelText("Company name");
    expect(input).not.toHaveAttribute("aria-describedby");
    expect(input).not.toHaveAttribute("aria-invalid");
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("merges an existing aria-describedby on the child", () => {
    render(
      <FormField label="Company name" htmlFor="company" hint="As registered">
        <Input id="company" aria-describedby="external-note" />
      </FormField>,
    );
    expect(screen.getByLabelText("Company name")).toHaveAttribute(
      "aria-describedby",
      "external-note company-hint",
    );
  });
});
