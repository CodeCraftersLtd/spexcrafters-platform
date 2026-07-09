import { NextResponse } from 'next/server';

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

/** Claim a submitted application for review (requires supplier.review.claim). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { applicationId } = await context.params;
  try {
    const detail = await createServerApiClient(session.accessToken).claimReview(
      applicationId,
    );
    return NextResponse.json(detail, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
