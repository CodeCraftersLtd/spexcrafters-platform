import { NextResponse } from 'next/server';

import type { ReasonRequest } from '@spexcrafters/api-client';

import { readJsonBody } from '@/lib/bff';
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

/** Revoke a verification scope (requires supplier.verification.revoke). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { supplierId, scopeCode } = await context.params;
  const body = (await readJsonBody<ReasonRequest>(request)) ?? undefined;
  try {
    const status = await createServerApiClient(
      session.accessToken,
    ).revokeVerificationScope(supplierId, scopeCode, body);
    return NextResponse.json(status, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
