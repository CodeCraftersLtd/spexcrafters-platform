import type { ReactNode } from "react";
import { cx } from "../cx";
import styles from "./Alert.module.css";

export interface AlertProps {
  tone: "info" | "success" | "warning" | "danger";
  title?: string;
  children: ReactNode;
}

/**
 * Inline status message. Icon-free v1: tone is conveyed by the border, the
 * subtle background and the title/body text (AA pairs from design-system.md
 * §2.5), so color is never the sole carrier — the text says what the tone is.
 *
 * `role="status"` (polite) for info/success; `role="alert"` for
 * warning/danger — alerts are reserved for things needing attention (§7.8).
 */
export function Alert({ tone, title, children }: AlertProps) {
  const role = tone === "warning" || tone === "danger" ? "alert" : "status";

  return (
    <div className={cx(styles.alert, styles[tone])} role={role}>
      {title ? <p className={styles.title}>{title}</p> : null}
      <div className={styles.body}>{children}</div>
    </div>
  );
}
