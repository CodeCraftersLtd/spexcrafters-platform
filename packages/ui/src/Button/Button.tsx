"use client";

import { forwardRef } from "react";
import type { ButtonHTMLAttributes, MouseEvent } from "react";
import { cx } from "../cx";
import { Spinner } from "../Spinner/Spinner";
import styles from "./Button.module.css";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "quiet" | "destructive";
  size?: "sm" | "md";
  /**
   * Loading state (design-system.md §5): an inline spinner replaces the
   * label while the label keeps occupying space, so the button's width is
   * preserved (no layout shift). The control gets `aria-busy="true"` and the
   * action is locked — clicks are swallowed, preventing double-submit. The
   * button stays focusable (unlike `disabled`), so keyboard focus is not lost.
   */
  loading?: boolean;
}

/**
 * Single action trigger (design-system.md §6.1) — a real `<button>`, one
 * primary per view. `type` defaults to "button" so forms are never submitted
 * by accident. Full §5 state matrix: hover/active shift one ramp step,
 * focus-visible shows the system ring, disabled uses tokens (never opacity
 * on text), loading preserves width and sets `aria-busy`.
 */
export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  function Button(
    {
      variant = "primary",
      size = "md",
      loading = false,
      type = "button",
      className,
      children,
      onClick,
      ...rest
    },
    ref,
  ) {
    const handleClick = (event: MouseEvent<HTMLButtonElement>) => {
      if (loading) {
        // Lock the action while busy — no double-submit.
        event.preventDefault();
        return;
      }
      onClick?.(event);
    };

    return (
      <button
        {...rest}
        ref={ref}
        type={type}
        className={cx(styles.button, styles[variant], styles[size], className)}
        aria-busy={loading || undefined}
        data-loading={loading || undefined}
        onClick={handleClick}
      >
        <span className={styles.label}>{children}</span>
        {loading ? (
          <span className={styles.spinnerOverlay} data-size={size}>
            <Spinner size={size} label="Loading" />
          </span>
        ) : null}
      </button>
    );
  },
);
