import { fireEvent, render, screen } from "@testing-library/react";
import { createRef } from "react";
import { describe, expect, it, vi } from "vitest";
import { Button } from "./Button";

describe("Button", () => {
  it("renders a real <button> defaulting to type=button", () => {
    render(<Button>Save</Button>);
    const button = screen.getByRole("button", { name: /save/i });
    expect(button.tagName).toBe("BUTTON");
    expect(button).toHaveAttribute("type", "button");
  });

  it("fires onClick in the default state", () => {
    const onClick = vi.fn();
    render(<Button onClick={onClick}>Save</Button>);
    fireEvent.click(screen.getByRole("button", { name: /save/i }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("blocks clicks when disabled", () => {
    const onClick = vi.fn();
    render(
      <Button disabled onClick={onClick}>
        Save
      </Button>,
    );
    const button = screen.getByRole("button", { name: /save/i });
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(onClick).not.toHaveBeenCalled();
  });

  it("sets aria-busy and locks the action while loading", () => {
    const onClick = vi.fn();
    render(
      <Button loading onClick={onClick}>
        Save
      </Button>,
    );
    const button = screen.getByRole("button");
    expect(button).toHaveAttribute("aria-busy", "true");
    // Stays focusable (not disabled) but the action is locked.
    expect(button).not.toBeDisabled();
    fireEvent.click(button);
    expect(onClick).not.toHaveBeenCalled();
  });

  it("shows a spinner while loading but keeps the label in the DOM (width preserved)", () => {
    render(<Button loading>Save</Button>);
    const button = screen.getByRole("button");
    expect(screen.getByText("Save")).toBeInTheDocument();
    expect(button.querySelector('[role="status"]')).not.toBeNull();
  });

  it("does not render a spinner when not loading", () => {
    render(<Button>Save</Button>);
    const button = screen.getByRole("button");
    expect(button).not.toHaveAttribute("aria-busy");
    expect(button.querySelector('[role="status"]')).toBeNull();
  });

  it("forwards its ref to the underlying button element", () => {
    const ref = createRef<HTMLButtonElement>();
    render(<Button ref={ref}>Save</Button>);
    expect(ref.current).toBeInstanceOf(HTMLButtonElement);
  });
});
