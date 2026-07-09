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
  params: Promise<{ supplierId: string }>;
}

/** Suspend an operational supplier (requires supplier.suspend). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { supplierId } = await context.params;
  const body = (await readJsonBody<ReasonRequest>(request)) ?? undefined;
  try {
    await createServerApiClient(session.accessToken).suspendSupplier(supplierId, body);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
