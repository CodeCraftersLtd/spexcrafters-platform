import { NextResponse } from 'next/server';

import type { FinalizeUploadRequest } from '@spexcrafters/api-client';

import { readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

interface RouteContext {
  params: Promise<{ supplierId: string; evidenceId: string }>;
}

/** Finalize an upload (idempotent; requires supplier.evidence.upload). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { supplierId, evidenceId } = await context.params;
  // Body is optional (may carry expectedSha256); tolerate an empty/missing body.
  const body = (await readJsonBody<FinalizeUploadRequest>(request)) ?? undefined;
  try {
    const evidence = await createServerApiClient(
      session.accessToken,
    ).finalizeEvidenceUpload(supplierId, evidenceId, body);
    return NextResponse.json(evidence, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
