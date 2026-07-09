import { NextResponse } from 'next/server';

import type { GrantScopeRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

interface RouteContext {
  params: Promise<{ supplierId: string; scopeCode: string }>;
}

/** Grant a verification scope with evidence linkage (supplier.verification.grant). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { supplierId, scopeCode } = await context.params;
  const body = await readJsonBody<GrantScopeRequest>(request);
  if (!body || !Array.isArray(body.evidenceIds) || body.evidenceIds.length === 0) {
    return invalidRequestBody();
  }
  try {
    const status = await createServerApiClient(
      session.accessToken,
    ).grantVerificationScope(supplierId, scopeCode, body);
    return NextResponse.json(status, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
