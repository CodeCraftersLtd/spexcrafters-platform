import { NextResponse } from 'next/server';

import type { ChangeRoleRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
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

const ROLES = new Set(['OWNER', 'ADMIN', 'MEMBER']);

export async function PUT(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }

  const { id, membershipId } = await context.params;
  const body = await readJsonBody<ChangeRoleRequest>(request);
  if (!body || !ROLES.has(body.role as string)) {
    return invalidRequestBody();
  }

  try {
    const member = await createServerApiClient(session.accessToken).changeMemberRole(
      id,
      membershipId,
      { role: body.role },
    );
    return NextResponse.json(member, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
