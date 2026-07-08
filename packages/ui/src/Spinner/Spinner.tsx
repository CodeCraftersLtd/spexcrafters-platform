import { cx } from "../cx";
import { VisuallyHidden } from "../VisuallyHidden/VisuallyHidden";
import styles from "./Spinner.module.css";

export interface SpinnerProps {
  /**
   * Visually hidden label announced by assistive technology
   * (e.g. "Loading quotes…"). Defaults to "Loading…".
   */
  label?: string;
  size?: "sm" | "md";
}

/**
 * Action-scale busy indicator (design-system.md §6.1): `role="status"` +
 * visually hidden label. Skeleton is for content; Spinner is for actions.
 * The rotation stops under `prefers-reduced-motion` (static arc remains).
 */
export function Spinner({ label = "Loading…", size = "md" }: SpinnerProps) {
  return (
    <span className={cx(styles.root, styles[size])} role="status">
      <span className={styles.ring} aria-hidden="true" />
      <VisuallyHidden>{label}</VisuallyHidden>
    </span>
  );
}
