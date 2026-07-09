import { NextResponse } from 'next/server';

import type { UpdateSupplierDraftRequest } from '@spexcrafters/api-client';

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

/** Update draft content (requires supplier.update; optimistic version). */
export async function PATCH(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }

  const { applicationId } = await context.params;
  const body = await readJsonBody<UpdateSupplierDraftRequest>(request);
  if (!body || typeof body.version !== 'number') {
    return invalidRequestBody();
  }

  try {
    const application = await createServerApiClient(
      session.accessToken,
    ).updateSupplierApplicationDraft(applicationId, body);
    return NextResponse.json(application, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
