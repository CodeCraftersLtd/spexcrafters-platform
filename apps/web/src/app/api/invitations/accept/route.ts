import { NextResponse } from 'next/server';

import type { AcceptInvitationRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import { apiErrorResponse, isErrorResponse, requireApiSession } from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

export async function POST(request: Request): Promise<NextResponse> {
  const session = await requireApiSession();
  if (isErrorResponse(session)) {
    return session;
  }

  const body = await readJsonBody<AcceptInvitationRequest>(request);
  if (!body || typeof body.token !== 'string' || body.token.length === 0) {
    return invalidRequestBody();
  }

  try {
    const membership = await createServerApiClient(
      session.accessToken,
    ).acceptInvitation({ token: body.token });
    return NextResponse.json(membership, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
