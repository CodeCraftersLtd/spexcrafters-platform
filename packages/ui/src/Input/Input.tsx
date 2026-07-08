"use client";

import { forwardRef } from "react";
import type { InputHTMLAttributes } from "react";
import { cx } from "../cx";
import styles from "./Input.module.css";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  /**
   * Marks the field invalid: error border token + `aria-invalid="true"`.
   * Pair with an error message wired via `aria-describedby` — color is never
   * the sole indicator (design-system.md §5). `FormField` does both for you.
   */
  invalid?: boolean;
}

/**
 * Single-line text entry (design-system.md §6.1). Always pair with a
 * programmatic `<label>` — placeholder is never a label. Uses
 * `--sc-border-strong` (≥3:1 meaningful boundary, WCAG 1.4.11) and the
 * `--sc-border-error` token color when invalid.
 */
export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { invalid = false, className, ...rest },
  ref,
) {
  return (
    <input
      {...rest}
      ref={ref}
      className={cx(styles.input, className)}
      aria-invalid={invalid ? true : rest["aria-invalid"]}
    />
  );
});
