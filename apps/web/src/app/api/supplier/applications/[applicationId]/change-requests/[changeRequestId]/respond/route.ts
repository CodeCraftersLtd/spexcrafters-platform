import { NextResponse } from 'next/server';

import type { RespondChangeRequestRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

interface RouteContext {
  params: Promise<{ applicationId: string; changeRequestId: string }>;
}

/** Respond to a change request (requires supplier.update). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { applicationId, changeRequestId } = await context.params;
  const body = await readJsonBody<RespondChangeRequestRequest>(request);
  if (!body || typeof body.response !== 'string') {
    return invalidRequestBody();
  }
  try {
    const result = await createServerApiClient(
      session.accessToken,
    ).respondToChangeRequest(applicationId, changeRequestId, body);
    return NextResponse.json(result, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
