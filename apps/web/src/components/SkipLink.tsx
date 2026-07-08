import styles from './SkipLink.module.css';

interface SkipLinkProps {
  /** Localized "Skip to main content" label. */
  label: string;
}

/**
 * First focusable element in the document (design system §7.2). Visually
 * hidden until it receives keyboard focus.
 */
export function SkipLink({ label }: SkipLinkProps) {
  return (
    <a className={styles.skipLink} href="#main-content">
      {label}
    </a>
  );
}
