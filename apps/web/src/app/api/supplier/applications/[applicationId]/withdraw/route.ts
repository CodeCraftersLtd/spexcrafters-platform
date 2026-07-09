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

/** Withdraw the application (requires supplier.withdraw; frees the org slot). */
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
    const application = await createServerApiClient(
      session.accessToken,
    ).withdrawSupplierApplication(applicationId);
    return NextResponse.json(application, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
