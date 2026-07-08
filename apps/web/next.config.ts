import type { NextConfig } from 'next';

const isDev = process.env.NODE_ENV !== 'production';

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
  isDev ? "connect-src 'self' ws:" : "connect-src 'self'",
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

export default nextConfig;
