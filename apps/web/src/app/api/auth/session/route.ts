import { NextResponse } from 'next/server';

import { getSession, refreshIfNeeded } from '@/lib/session';

export const runtime = 'nodejs';

/**
 * Returns the current session's user, transparently rotating the token pair
 * (and the sc_session cookie) when the access token is close to expiry.
 */
export async function GET(): Promise<NextResponse> {
  const session = await getSession();
  if (!session) {
    return NextResponse.json(
      { error: { code: 'authentication-failed', message: '' } },
      { status: 401 },
    );
  }

  const fresh = await refreshIfNeeded(session);
  if (!fresh) {
    return NextResponse.json(
      { error: { code: 'authentication-failed', message: '' } },
      { status: 401 },
    );
  }

  return NextResponse.json({ user: fresh.user }, { status: 200 });
}
