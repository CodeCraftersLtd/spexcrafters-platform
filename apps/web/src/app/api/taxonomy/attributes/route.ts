import { NextResponse } from 'next/server';

import type { CreateAttributeRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

/** Create a specification attribute (platform-staff: TAXONOMY_WRITE). */
export async function POST(request: Request): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const body = await readJsonBody<CreateAttributeRequest>(request);
  if (
    !body ||
    typeof body.code !== 'string' ||
    typeof body.dataType !== 'string' ||
    typeof body.name !== 'string' ||
    typeof body.originalLocale !== 'string'
  ) {
    return invalidRequestBody();
  }
  try {
    const detail = await createServerApiClient(session.accessToken).createAttribute(body);
    return NextResponse.json(detail, { status: 201 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
