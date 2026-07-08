"use client";

import { Children, cloneElement, isValidElement } from "react";
import type { ReactElement, ReactNode } from "react";
import styles from "./FormField.module.css";

export interface FormFieldProps {
  /** Visible label text — labels are always visible (WCAG 3.3.2). */
  label: string;
  /** Id of the control; the label's `for`, hint id (`${htmlFor}-hint`) and
   *  error id (`${htmlFor}-error`) derive from it. */
  htmlFor: string;
  hint?: string;
  /** Error message. Rendered with `role="alert"`; persists until corrected. */
  error?: string;
  children: ReactNode;
}

/**
 * Label + control + hint/error wiring (design-system.md §5 error state).
 *
 * Accessibility contract: the hint gets id `${htmlFor}-hint`, the error gets
 * id `${htmlFor}-error` with `role="alert"`. Valid element children are
 * cloned to receive `aria-describedby` (error id first, merged with any
 * existing value) and, when an error is present, `aria-invalid` — so the
 * wiring is automatic. Non-element children are left untouched; consumers
 * composing custom controls can also apply the same ids manually.
 */
export function FormField({ label, htmlFor, hint, error, children }: FormFieldProps) {
  const hintId = hint ? `${htmlFor}-hint` : undefined;
  const errorId = error ? `${htmlFor}-error` : undefined;
  const describedBy = [errorId, hintId].filter(Boolean).join(" ") || undefined;

  const content = Children.map(children, (child) => {
    if (!isValidElement(child)) return child;
    const element = child as ReactElement<Record<string, unknown>>;
    const existing = element.props["aria-describedby"];
    const merged =
      [typeof existing === "string" ? existing : undefined, describedBy]
        .filter(Boolean)
        .join(" ") || undefined;

    const injected: Record<string, unknown> = {};
    if (merged) injected["aria-describedby"] = merged;
    if (error) injected["aria-invalid"] = true;
    return Object.keys(injected).length > 0
      ? cloneElement(element, injected)
      : element;
  });

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={htmlFor}>
        {label}
      </label>
      {content}
      {hint ? (
        <p className={styles.hint} id={hintId}>
          {hint}
        </p>
      ) : null}
      {error ? (
        <p className={styles.error} id={errorId} role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}
