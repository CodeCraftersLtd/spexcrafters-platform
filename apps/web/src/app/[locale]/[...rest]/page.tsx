import { notFound } from 'next/navigation';

/**
 * Catch-all for unknown paths under a valid locale prefix. Delegates to the
 * localized not-found boundary so 404s render inside the locale layout.
 */
export default function CatchAllNotFound(): never {
  notFound();
}
