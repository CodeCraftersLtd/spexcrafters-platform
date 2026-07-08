import { NextResponse } from 'next/server';

import type { CreateOrganizationRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

const ORGANIZATION_TYPES = new Set(['BUYER', 'SUPPLIER', 'HYBRID']);

export async function POST(request: Request): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }

  const body = await readJsonBody<CreateOrganizationRequest>(request);
  if (
    !body ||
    typeof body.name !== 'string' ||
    body.name.trim().length === 0 ||
    !ORGANIZATION_TYPES.has(body.type as string) ||
    (body.country !== undefined && typeof body.country !== 'string')
  ) {
    return invalidRequestBody();
  }

  const payload: CreateOrganizationRequest = {
    name: body.name.trim(),
    type: body.type,
    ...(body.country ? { country: body.country } : {}),
  };

  try {
    const organization = await createServerApiClient(
      session.accessToken,
    ).createOrganization(payload);
    return NextResponse.json(organization, { status: 201 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
