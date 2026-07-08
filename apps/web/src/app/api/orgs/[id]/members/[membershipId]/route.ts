import { NextResponse } from 'next/server';

import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

interface RouteContext {
  params: Promise<{ id: string; membershipId: string }>;
}

export async function DELETE(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }

  const { id, membershipId } = await context.params;

  try {
    await createServerApiClient(session.accessToken).removeMember(id, membershipId);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
