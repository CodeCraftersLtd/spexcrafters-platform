import { NextResponse } from 'next/server';

import type { UpdateOrganizationRequest } from '@spexcrafters/api-client';

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

export async function PATCH(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }

  const { id } = await context.params;
  const body = await readJsonBody<UpdateOrganizationRequest>(request);
  if (
    !body ||
    typeof body.version !== 'number' ||
    (body.name !== undefined && typeof body.name !== 'string') ||
    (body.country !== undefined && typeof body.country !== 'string')
  ) {
    return invalidRequestBody();
  }

  const payload: UpdateOrganizationRequest = {
    version: body.version,
    ...(body.name !== undefined ? { name: body.name.trim() } : {}),
    ...(body.country !== undefined ? { country: body.country } : {}),
  };

  try {
    const organization = await createServerApiClient(
      session.accessToken,
    ).updateOrganization(id, payload);
    return NextResponse.json(organization, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
