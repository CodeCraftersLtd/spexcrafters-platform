import { NextResponse } from 'next/server';

import type { ReasonRequest } from '@spexcrafters/api-client';

import { readJsonBody } from '@/lib/bff';
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

/** Reject the application (requires supplier.review.reject; optional reason). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { applicationId } = await context.params;
  const body = (await readJsonBody<ReasonRequest>(request)) ?? undefined;
  try {
    const detail = await createServerApiClient(
      session.accessToken,
    ).rejectSupplierApplication(applicationId, body);
    return NextResponse.json(detail, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
