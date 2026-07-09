import { NextResponse } from 'next/server';

import type { PutSpecificationTemplateRequest } from '@spexcrafters/api-client';

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

/** Replace a category's specification template (platform-staff: TAXONOMY_WRITE). */
export async function PUT(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { id } = await context.params;
  const body = await readJsonBody<PutSpecificationTemplateRequest>(request);
  if (!body || typeof body.code !== 'string' || !Array.isArray(body.attributes)) {
    return invalidRequestBody();
  }
  try {
    const template = await createServerApiClient(
      session.accessToken,
    ).putSpecificationTemplate(id, body);
    return NextResponse.json(template, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
