import { NextResponse } from 'next/server';

import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSession,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

/** Header carrying the scanner verdict; surfaced to the client as a banner. */
const SCAN_STATE_HEADER = 'x-evidence-scan-state';

interface RouteContext {
  params: Promise<{ supplierId: string; evidenceId: string }>;
}

/**
 * Stream an authorized evidence download by proxying the backend's stream
 * (org supplier.evidence.read for the owning org, or platform
 * supplier.review.read). GET is a safe method — no CSRF. The backend is the
 * authority and fails closed to downloadable states; this route never exposes
 * the storage key. The scan-state header is forwarded so the client can render
 * the "not malware-scanned" banner (Phase 7 never marks evidence CLEAN).
 */
export async function GET(
  _request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSession();
  if (isErrorResponse(session)) {
    return session;
  }
  const { supplierId, evidenceId } = await context.params;
  try {
    const upstream = await createServerApiClient(
      session.accessToken,
    ).downloadEvidence(supplierId, evidenceId);

    const headers = new Headers();
    for (const name of [
      'content-type',
      'content-length',
      'content-disposition',
      SCAN_STATE_HEADER,
    ]) {
      const value = upstream.headers.get(name);
      if (value) {
        headers.set(name, value);
      }
    }
    // Never cache a private, authorized stream in a shared cache.
    headers.set('cache-control', 'no-store');
    return new NextResponse(upstream.body, { status: 200, headers });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
