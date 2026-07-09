import { NextResponse } from 'next/server';

import type { CreateUnitRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

/** Create a unit of measurement (platform-staff: TAXONOMY_WRITE). */
export async function POST(request: Request): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const body = await readJsonBody<CreateUnitRequest>(request);
  if (
    !body ||
    typeof body.code !== 'string' ||
    typeof body.family !== 'string' ||
    typeof body.displayName !== 'string' ||
    typeof body.originalLocale !== 'string'
  ) {
    return invalidRequestBody();
  }
  try {
    const unit = await createServerApiClient(session.accessToken).createUnit(body);
    return NextResponse.json(unit, { status: 201 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
