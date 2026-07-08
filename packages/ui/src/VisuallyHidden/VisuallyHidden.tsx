import type { ReactNode } from "react";
import styles from "./VisuallyHidden.module.css";

export interface VisuallyHiddenProps {
  children: ReactNode;
}

/**
 * AT-only text utility (design-system.md §6.1): standard clip pattern —
 * never `display: none`, so screen readers still announce the content.
 */
export function VisuallyHidden({ children }: VisuallyHiddenProps) {
  return <span className={styles.root}>{children}</span>;
}
