import { NextResponse } from 'next/server';

import type { AddEnumerationValueRequest } from '@spexcrafters/api-client';

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

/** Add a value to an enumeration (platform-staff: TAXONOMY_WRITE). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { id } = await context.params;
  const body = await readJsonBody<AddEnumerationValueRequest>(request);
  if (
    !body ||
    typeof body.code !== 'string' ||
    typeof body.label !== 'string' ||
    typeof body.originalLocale !== 'string'
  ) {
    return invalidRequestBody();
  }
  try {
    const value = await createServerApiClient(
      session.accessToken,
    ).addEnumerationValue(id, body);
    return NextResponse.json(value, { status: 201 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
