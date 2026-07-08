import { NextResponse } from 'next/server';

import { apiFetch } from '@/lib/bff';
import { destroySession, getSession } from '@/lib/session';

export const runtime = 'nodejs';

export async function POST(): Promise<NextResponse> {
  const session = await getSession();

  if (session) {
    // Best effort: revoke the refresh-token family upstream. The local
    // session is destroyed regardless, so a transient upstream failure
    // never blocks sign-out.
    try {
      await apiFetch('/auth/logout', { refreshToken: session.refreshToken });
    } catch {
      // Intentionally ignored — upstream revocation is idempotent and the
      // token family expires server-side on its own schedule.
    }
  }

  await destroySession();
  return new NextResponse(null, { status: 204 });
}
