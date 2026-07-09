import { Client } from 'pg';

/**
 * DOCUMENTED STAFF-BOOTSTRAP FIXTURE (the ONLY permitted direct DB mutation in
 * the E2E suite).
 *
 * The reviewer surface is authorized by platform staff capabilities, which have
 * no self-service enrollment API in Phase 7 — a reviewer is provisioned out of
 * band. To exercise Journey B (reviewer) end-to-end, this helper inserts the
 * test user into `platform_access.platform_staff` so the backend grants the
 * supplier.review.* capabilities on the user's next token.
 *
 * Scope discipline: this touches ONLY the staff-provisioning table. It never
 * mutates any application/supplier/evidence/verification table — all of that
 * state is created through the public API in the specs. Runs against
 * DATABASE_URL (defaulting to the local compose Postgres).
 */

const DATABASE_URL =
  process.env.DATABASE_URL ??
  'postgresql://spex:spex@localhost:5432/spexcrafters';

/**
 * Grant the given user id platform-reviewer staff access. Idempotent: a second
 * call for the same user is a no-op. Returns nothing; throws if the DB is
 * unreachable so the spec fails loudly rather than silently skipping the grant.
 */
export async function provisionReviewer(
  userId: string,
  options: { role?: string } = {},
): Promise<void> {
  const role = options.role ?? 'REVIEWER';
  const client = new Client({ connectionString: DATABASE_URL });
  await client.connect();
  try {
    // The staff table is keyed by user id; ON CONFLICT keeps this idempotent.
    await client.query(
      `INSERT INTO platform_access.platform_staff
         (id, user_id, platform_role, active, created_at, updated_at, version)
       VALUES (gen_random_uuid(), $1, $2, true, now(), now(), 0)
       ON CONFLICT (user_id)
         DO UPDATE SET platform_role = EXCLUDED.platform_role, active = true, updated_at = now()`,
      [userId, role],
    );
  } finally {
    await client.end();
  }
}
