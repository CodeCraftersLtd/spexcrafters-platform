'use client';

import errors from '../../messages/en/errors.json';

/**
 * Last-resort error boundary: replaces the root layout entirely, so it must
 * render its own <html>/<body>. Messages cannot be negotiated at this point —
 * the default-locale (en) copy is used directly. Styles are inline because the
 * token stylesheet may not have loaded when this boundary renders.
 */
interface GlobalErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function GlobalError({ reset }: GlobalErrorProps) {
  const copy = errors.unexpected;

  return (
    <html lang="en">
      <body
        style={{
          margin: 0,
          minHeight: '100dvh',
          display: 'grid',
          placeItems: 'center',
          fontFamily:
            "-apple-system, 'Segoe UI', system-ui, Arial, sans-serif",
          background: '#FDFDFB',
          color: '#14171C',
        }}
      >
        <main style={{ textAlign: 'center', padding: '2rem' }}>
          <h1 style={{ fontSize: '1.75rem', lineHeight: 1.3 }}>{copy.title}</h1>
          <p style={{ color: '#444D58' }}>{copy.body}</p>
          <button
            type="button"
            onClick={() => reset()}
            style={{
              marginTop: '1rem',
              padding: '0.5rem 1.25rem',
              background: '#2947B8',
              color: '#FDFDFB',
              border: 0,
              borderRadius: '4px',
              font: 'inherit',
              cursor: 'pointer',
            }}
          >
            {copy.retry}
          </button>
        </main>
      </body>
    </html>
  );
}
