import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Alert } from "./Alert";

describe("Alert", () => {
  it('uses role="status" for info', () => {
    render(<Alert tone="info">Prices shown in EUR.</Alert>);
    expect(screen.getByRole("status")).toHaveTextContent("Prices shown in EUR.");
  });

  it('uses role="status" for success', () => {
    render(<Alert tone="success">RFQ sent.</Alert>);
    expect(screen.getByRole("status")).toHaveTextContent("RFQ sent.");
  });

  it('uses role="alert" for warning', () => {
    render(<Alert tone="warning">Certificate expiring.</Alert>);
    expect(screen.getByRole("alert")).toHaveTextContent("Certificate expiring.");
  });

  it('uses role="alert" for danger', () => {
    render(<Alert tone="danger">Quotation failed.</Alert>);
    expect(screen.getByRole("alert")).toHaveTextContent("Quotation failed.");
  });

  it("renders the title text so tone is never conveyed by color alone", () => {
    render(
      <Alert tone="danger" title="Quotation failed">
        Retry sending the request.
      </Alert>,
    );
    const alert = screen.getByRole("alert");
    expect(alert).toHaveTextContent("Quotation failed");
    expect(alert).toHaveTextContent("Retry sending the request.");
  });
});
