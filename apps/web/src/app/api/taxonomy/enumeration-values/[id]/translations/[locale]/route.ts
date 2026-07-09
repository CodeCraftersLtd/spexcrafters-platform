import { NextResponse } from 'next/server';

import type { TranslationUpsertRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

interface RouteContext {
  params: Promise<{ id: string; locale: string }>;
}

/** Upsert an enumeration-value translation (platform-staff: TAXONOMY_WRITE). */
export async function PUT(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { id, locale } = await context.params;
  const body = await readJsonBody<TranslationUpsertRequest>(request);
  if (!body || typeof body.name !== 'string' || typeof body.source !== 'string') {
    return invalidRequestBody();
  }
  try {
    const view = await createServerApiClient(
      session.accessToken,
    ).upsertEnumerationValueTranslation(id, locale, body);
    return NextResponse.json(view, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
