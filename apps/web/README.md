# @spexcrafters/web

SpexCrafters web application — public site, authentication, and buyer portal. Next.js App Router with a BFF (backend-for-frontend) layer that holds all API tokens server-side.

## Prerequisites

- **Node 22 LTS**
- **pnpm 9** (`corepack enable && corepack prepare pnpm@9 --activate`)
- For the full local stack (needed by the e2e suite):
  - SpexCrafters API (Spring Boot) on `http://localhost:8080`
  - Mailpit on `http://localhost:8025` (verification emails)

## Environment variables

Copy `.env.example` to `.env.local` and fill in:

| Variable | Purpose | Default |
|---|---|---|
| `API_BASE_URL` | Upstream SpexCrafters API base | `http://localhost:8080/api/v1` |
| `SESSION_SECRET` | 32 bytes, base64 — encrypts the `sc_session` cookie | — (required) |
| `NEXT_PUBLIC_SITE_URL` | Canonical origin for absolute metadata URLs | `http://localhost:3000` |

Generate a session secret:

```sh
node -e "console.log(require('node:crypto').randomBytes(32).toString('base64'))"
```

## Commands

```sh
pnpm dev          # dev server on :3000
pnpm build        # production build
pnpm start        # serve the production build
pnpm lint         # eslint (next/core-web-vitals)
pnpm typecheck    # next typegen + tsc --noEmit (typed routes)
pnpm test         # vitest unit tests (schemas, session JWE, LoginForm)
pnpm e2e          # playwright smoke suite (requires API + Mailpit running)
```

The e2e base URL can be overridden with `PLAYWRIGHT_BASE_URL` (defaults to `http://localhost:3000`); Mailpit with `MAILPIT_URL`.

## Architecture notes

### BFF & sessions

The browser never sees API tokens. Route handlers under `src/app/api/auth/*` (Node runtime) proxy to the upstream API, translate RFC 9457 `problem+json` into a uniform `{ error: { code, message, fields? } }` envelope, and on login store the token pair in an **encrypted, HttpOnly `sc_session` cookie** (JWE `dir`/`A256GCM` via `jose`, key from `SESSION_SECRET`). `src/lib/session.ts` exposes `createSession` / `getSession` / `destroySession` / `refreshIfNeeded`; refresh rotates the token pair (and cookie) when the access token is within 60 s of expiry, and is only invoked from cookie-mutation contexts (route handlers). `GET /api/auth/session` returns the current user and performs that rotation.

### i18n

In-house dictionary i18n — no library. Locales: `en`, `zh-Hans`, `fr`, `de` (`messages/*.json`; `en` is the schema, the `Dictionary` type is derived from it; the other locales currently carry English values pending translation). `src/middleware.ts` negotiates a locale from `Accept-Language` and redirects `/…` → `/{locale}/…`, and applies a cookie-presence guard on `/{locale}/buyer/**` (real enforcement happens server-side in the buyer layout via `getSession()`). All user-facing strings come from the dictionary; `interpolate()` handles `{placeholder}` substitution.

### Styling

CSS Modules + design tokens only. `@spexcrafters/design-tokens/css` is imported once in the `[locale]` root layout; component styles reference `var(--sc-…)` semantic tokens exclusively (no raw hex/px except `0`/`1px`). Global focus-visible ring, skip links, and the interaction-state rules follow `docs/design-system/design-system.md`. Interactive primitives come from `@spexcrafters/ui` (Button, Input, FormField, Alert).

### Routes

```
src/app/
  [locale]/               root layout (html lang, tokens css)
    (public)/             header + footer marketing shell, homepage
    (auth)/auth/          centered card: register, login, verify-email
    (buyer)/buyer/        app shell, session-guarded dashboard
    [...rest]/            localized 404 catch-all
  api/auth/*              BFF route handlers (register, login, logout,
                          verify-email, resend-verification, session)
```

Auth and buyer segments ship `robots: noindex`. Page titles use the `%s · SpexCrafters` template.

### Versions

Dependencies are caret-pinned to known-stable lines (Jan 2026): Next `^16`, React `^19.1`, TypeScript `^5.7`, zod `^3.24`, react-hook-form `^7.54` + `@hookform/resolvers` `^3.10`, jose `^6`, vitest `^3`, Playwright `^1.50`. Workspace packages (`@spexcrafters/design-tokens`, `@spexcrafters/ui`, `@spexcrafters/api-client`, `@spexcrafters/config`) resolve via `workspace:*`.
