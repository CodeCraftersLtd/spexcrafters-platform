import { NextResponse } from 'next/server';

import { apiFetch } from '@/lib/bff';
import { enforceCsrf } from '@/lib/csrf';
import { destroySession, getSession } from '@/lib/session';

export const runtime = 'nodejs';

export async function POST(request: Request): Promise<NextResponse> {
  const session = await getSession();

  if (session) {
    // Logout is a session-destroying mutation: a cross-site forced logout is a
    // real attack, so the CSRF guard must BLOCK it (403) rather than proceed.
    const csrfError = enforceCsrf(request, session);
    if (csrfError) {
      return csrfError;
    }

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
