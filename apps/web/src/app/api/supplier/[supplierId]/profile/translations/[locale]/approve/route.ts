import { NextResponse } from 'next/server';

import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

interface RouteContext {
  params: Promise<{ supplierId: string; locale: string }>;
}

/** Approve a profile translation (requires supplier.update). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { supplierId, locale } = await context.params;
  try {
    const translation = await createServerApiClient(
      session.accessToken,
    ).approveProfileTranslation(supplierId, locale);
    return NextResponse.json(translation, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
