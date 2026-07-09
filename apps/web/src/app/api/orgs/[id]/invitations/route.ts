import { NextResponse } from 'next/server';

import type { CreateInvitationRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

interface RouteContext {
  params: Promise<{ id: string }>;
}

const INVITABLE_ROLES = new Set(['ADMIN', 'MEMBER']);

export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }

  const { id } = await context.params;
  const body = await readJsonBody<CreateInvitationRequest>(request);
  if (
    !body ||
    typeof body.email !== 'string' ||
    body.email.trim().length === 0 ||
    !INVITABLE_ROLES.has(body.role as string)
  ) {
    return invalidRequestBody();
  }

  try {
    const invitation = await createServerApiClient(
      session.accessToken,
    ).createInvitation(id, { email: body.email.trim(), role: body.role });
    return NextResponse.json(invitation, { status: 201 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
