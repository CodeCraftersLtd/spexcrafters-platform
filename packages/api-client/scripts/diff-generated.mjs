// Fails if the committed generated schema drifts from a fresh generation.
// Used by the CI `contract` job after `openapi-typescript` writes schema.check.d.ts.
import { readFileSync, rmSync, existsSync } from 'node:fs';

const committed = 'src/generated/schema.d.ts';
const fresh = 'src/generated/schema.check.d.ts';

if (!existsSync(committed)) {
  console.log('No committed generated schema yet (bootstrap phase) — skipping diff.');
  rmSync(fresh, { force: true });
  process.exit(0);
}

const a = readFileSync(committed, 'utf8');
const b = readFileSync(fresh, 'utf8');
rmSync(fresh, { force: true });

if (a !== b) {
  console.error('Generated API schema is stale. Run: pnpm --filter @spexcrafters/api-client generate');
  process.exit(1);
}
console.log('Generated API schema is up to date.');
