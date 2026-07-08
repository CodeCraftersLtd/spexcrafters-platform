import type { NextConfig } from 'next';

const isDev = process.env.NODE_ENV !== 'production';

/**
 * Conservative, nonce-less Content-Security-Policy.
 *
 * Production keeps script-src at 'self'. Development additionally allows
 * 'unsafe-eval' and 'unsafe-inline' plus websocket connections, which the
 * Next.js dev server (React Refresh / HMR) requires to function at all.
 * A nonce-based policy replaces this once the edge/runtime nonce plumbing
 * lands in a later sprint.
 */
const contentSecurityPolicy = [
  "default-src 'self'",
  isDev ? "script-src 'self' 'unsafe-eval' 'unsafe-inline'" : "script-src 'self'",
  // Next.js injects <style> elements for CSS Modules during development and
  // for critical CSS inlining in production.
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
