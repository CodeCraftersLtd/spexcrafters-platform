import { NextResponse } from 'next/server';

import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

interface RouteContext {
  params: Promise<{ id: string; invitationId: string }>;
}

export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }

  const { id, invitationId } = await context.params;

  try {
    await createServerApiClient(session.accessToken).revokeInvitation(
      id,
      invitationId,
    );
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
