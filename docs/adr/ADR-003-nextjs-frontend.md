# ADR-003: Next.js 16+ (App Router) frontend

## Status
Accepted — 2026-07-08

## Context
SpexCrafters' web surface (`apps/web`) has two very different halves that must live in one coherent codebase: (1) a public, SEO-critical marketplace — supplier profiles, product pages with attribute-driven specs, category landing pages, insights/content — where search-engine visibility against established competitors (OpticLeague as the functional benchmark) is a primary acquisition channel and demands SSR/SSG/ISR, structured metadata, and locale-routed URLs; (2) three authenticated portals (buyer, supplier, admin) with rich interactivity (facet panels, quote composer, messaging, dashboards). The frontend also hosts the BFF that owns authentication cookies (ADR-006) and never talks to PostgreSQL directly.

## Decision
- **Next.js 16+ (App Router) + React + TypeScript strict** for all public pages and portals, in a pnpm-workspace monorepo alongside `packages/api-client` and `packages/design-tokens`.
- **Server Components by default**; Client Components only for genuine interactivity (facet panel, quote composer, messaging, notification/data tables).
- Rendering per route semantics: SSG + ISR for catalog and content pages, SSR for personalized pages; Next.js metadata API + locale routing for SEO/i18n.
- **BFF pattern:** Next.js route handlers own the auth flow, hold tokens server-side, and issue HttpOnly cookies (details in ADR-006); the browser never sees API tokens. Route handlers also proxy upload-ticket flows.
- Data access exclusively through the generated TypeScript client from `packages/api-client` (ADR-005); TanStack Query only inside interactive client islands; React Hook Form + Zod for complex forms.
- Styling via CSS Modules + design tokens (ADR-012 scope); no Tailwind foundation.
- Version policy: pin minor; adopt new majors ≥ 6 months after GA, with codemods and a visual-regression pass.
- **Self-hosted** as a Node/Docker container — no Vercel platform dependency.

## Alternatives considered
- **Remix / React Router (framework mode)** — rejected: solid app framework, but a weaker content-site story (no equivalent SSG/ISR maturity, thinner metadata/i18n routing conventions) for a platform whose growth depends on ranking thousands of public catalog pages.
- **Astro** — rejected: excellent for the content half, weak for the portal half (islands architecture strains under dashboard/messaging complexity); running Astro + a second app framework doubles the stack.
- **SvelteKit** — rejected: ecosystem and hiring-pool risk over a 5–8 year horizon; the React ecosystem (TanStack Query, RHF, generated clients, Storybook tooling) is a mandated asset.
- **SPA (Vite + React) + separate static site** — rejected: splits the codebase, loses unified locale routing and shared components, and makes SEO for dynamic catalog pages a bolt-on (prerender services) rather than a platform feature.

## Advantages
- The only mainstream option combining SSG/ISR/RSC, a first-class metadata API, and i18n routing at this maturity — exactly the public-page requirements.
- RSC keeps portal bundles small: server-rendered reads with client islands only where interaction demands it.
- One codebase, one deployment, one design-token pipeline for public site and all three portals.
- BFF colocation gives a natural, same-origin home for the cookie/session layer.

## Disadvantages
- Vercel-driven API churn: App Router conventions have historically evolved fast; majors can deprecate patterns we rely on.
- RSC's server/client mental model raises onboarding cost and constrains some library choices.
- Self-hosting Next.js forgoes some platform conveniences (image optimization CDN, edge middleware) that must be replicated at our CDN/edge layer.

## Risks
- **Framework churn breaks upgrade economics** → pin minors; wait ≥ 6 months post-GA on majors; rely on official codemods; the generated API client and CSS-Modules styling minimize framework-specific surface area.
- **Bundle growth in portals** → Server Components by default is enforced in review; Motion/GSAP restricted to client islands and the homepage hero route per the animation budget; CI bundle-size checks.
- **SEO regression during framework upgrades** → Playwright E2E journeys include metadata/structured-data assertions; staged rollout behind the CDN with sitemap monitoring.

## Migration path
The view layer is deliberately the most replaceable stratum:
- **Business logic lives behind the API** (ADR-005); the frontend owns presentation and the BFF only.
- **Styling is portable:** CSS Modules + design tokens have no Next.js coupling.
- **Data access is portable:** `packages/api-client` is plain `openapi-typescript` + `openapi-fetch`, usable from any TS framework.
- Replacement can proceed **at page granularity**: route another framework's output behind the same CDN path-by-path (public pages first, portals later), sharing tokens and the API client throughout. The BFF's cookie contract would be reimplemented once in the successor's server layer.
- Because the app is a standard Node/Docker container, hosting is never the lock-in vector.
