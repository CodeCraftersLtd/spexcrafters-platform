import type { NextConfig } from 'next';
import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin('./src/i18n/request.ts');

const isDev = process.env.NODE_ENV !== 'production';

/**
 * Object-storage origin(s) the browser uploads evidence to directly (ADR-023:
 * presigned direct-to-storage upload). `connect-src` MUST include the storage
 * origin or the browser blocks the presigned PUT with a CSP violation. This is
 * a deliberate, documented broadening of connect-src (the only broadening) —
 * set NEXT_PUBLIC_STORAGE_ORIGIN to the S3/R2/MinIO public origin per env.
 */
const storageOrigin = process.env.NEXT_PUBLIC_STORAGE_ORIGIN?.trim();
const connectSrc = [
  "'self'",
  isDev ? 'ws:' : null,
  storageOrigin || null,
]
  .filter(Boolean)
  .join(' ');

/**
 * Content-Security-Policy — v1 posture (SEC-DEBT-1, see docs/validation):
 *
 * script-src includes 'unsafe-inline' because public pages are statically
 * prerendered (SSG) and Next.js embeds its hydration bootstrap and Flight
 * data as inline scripts; per-request nonces cannot be injected into static
 * HTML, and 'strict-dynamic' would also block the 'self' chunk loads. A
 * script-src 'self'-only policy was validated to render pages but silently
 * prevent all hydration. Upgrading to nonce/hash-based CSP requires either
 * fully dynamic rendering or a hash-emission strategy — tracked as debt.
 * All other directives remain strict.
 */
const contentSecurityPolicy = [
  "default-src 'self'",
  isDev ? "script-src 'self' 'unsafe-eval' 'unsafe-inline'" : "script-src 'self' 'unsafe-inline'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data:",
  "font-src 'self'",
  `connect-src ${connectSrc}`,
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join('; ');

const securityHeaders = [
  { key: 'Content-Security-Policy', value: contentSecurityPolicy },
  { key: 'X-Content-Type-Options', value: 'nosniff' },
  { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
  { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=(), payment=()' },
  // HSTS is intentionally production-only: it must never be sent on plain
  // http://localhost during development.
  ...(isDev
    ? []
    : [{ key: 'Strict-Transport-Security', value: 'max-age=63072000; includeSubDomains' }]),
];

const nextConfig: NextConfig = {
  typedRoutes: true,
  poweredByHeader: false,
  // Workspace packages consumed directly from TypeScript source.
  transpilePackages: ['@spexcrafters/ui', '@spexcrafters/api-client'],
  // Required by infrastructure/docker/web.Dockerfile (copies .next/standalone).
  output: 'standalone',
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: securityHeaders,
      },
    ];
  },
};

export default withNextIntl(nextConfig);
