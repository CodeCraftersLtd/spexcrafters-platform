import { NextResponse } from 'next/server';

import type { RequestChangesRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

interface RouteContext {
  params: Promise<{ applicationId: string }>;
}

/** Request changes with a structured reason (supplier.review.request_changes). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { applicationId } = await context.params;
  const body = await readJsonBody<RequestChangesRequest>(request);
  if (
    !body ||
    typeof body.requestedItem !== 'string' ||
    typeof body.reason !== 'string'
  ) {
    return invalidRequestBody();
  }
  try {
    const detail = await createServerApiClient(
      session.accessToken,
    ).requestSupplierChanges(applicationId, body);
    return NextResponse.json(detail, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
