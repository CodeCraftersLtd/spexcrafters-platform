import { NextResponse } from 'next/server';

import type { InitiateUploadRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

interface RouteContext {
  params: Promise<{ supplierId: string }>;
}

/**
 * Initiate a presigned direct-to-storage upload (requires
 * supplier.evidence.upload). Returns the upload ticket (presigned PUT url +
 * required headers) to the client, which uploads bytes DIRECTLY to storage —
 * never through this BFF — then calls the finalize route.
 */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { supplierId } = await context.params;
  const body = await readJsonBody<InitiateUploadRequest>(request);
  if (
    !body ||
    typeof body.evidenceTypeCode !== 'string' ||
    typeof body.filename !== 'string' ||
    typeof body.mediaType !== 'string'
  ) {
    return invalidRequestBody();
  }
  try {
    const ticket = await createServerApiClient(
      session.accessToken,
    ).initiateEvidenceUpload(supplierId, body);
    return NextResponse.json(ticket, { status: 201 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
