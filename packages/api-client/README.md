# @spexcrafters/api-client

Typed TypeScript client for the SpexCrafters API.

- **Contract of record:** [`spec/openapi.json`](spec/openapi.json) (OpenAPI 3.1, committed). The Spring Boot application must stay compatible with it — the CI `contract` job compares the spec exported by the running application (`/v3/api-docs`) against this file and fails on breaking drift.
- **Generation:** `pnpm generate` runs `openapi-typescript` → `src/generated/schema.d.ts`. Never hand-edit generated output.
- **Bootstrap note:** `src/types.ts` was hand-authored to mirror the spec because the initial scaffold was produced without a Node toolchain. Once the first `generate` run is committed, `types.ts` must become re-exports of the generated schema (tracked in Sprint-2 backlog). Adding new endpoint types by hand is prohibited.
- **Usage:** server-side only (Next.js BFF). The browser never calls the API directly.

```ts
import { createApiClient, ApiProblemError } from '@spexcrafters/api-client';

const api = createApiClient({ baseUrl: process.env.API_BASE_URL! });
const tokens = await api.login({ email, password });
```
