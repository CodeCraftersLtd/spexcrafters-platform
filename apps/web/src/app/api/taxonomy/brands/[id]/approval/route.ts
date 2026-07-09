import { NextResponse } from 'next/server';

import type { BrandApprovalRequest } from '@spexcrafters/api-client';

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

/** Set a brand's approval status (platform-staff: BRAND_APPROVE). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { id } = await context.params;
  const body = await readJsonBody<BrandApprovalRequest>(request);
  if (!body || typeof body.status !== 'string') {
    return invalidRequestBody();
  }
  try {
    const detail = await createServerApiClient(
      session.accessToken,
    ).setBrandApproval(id, body);
    return NextResponse.json(detail, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
