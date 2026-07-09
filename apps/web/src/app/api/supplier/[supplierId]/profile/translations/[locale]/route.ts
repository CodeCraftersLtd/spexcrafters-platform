import { NextResponse } from 'next/server';

import type { UpsertTranslationRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
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

/** Create or update a profile translation (requires supplier.update). */
export async function PUT(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { supplierId, locale } = await context.params;
  const body = await readJsonBody<UpsertTranslationRequest>(request);
  if (!body || typeof body !== 'object') {
    return invalidRequestBody();
  }
  try {
    const translation = await createServerApiClient(
      session.accessToken,
    ).upsertProfileTranslation(supplierId, locale, body);
    return NextResponse.json(translation, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
