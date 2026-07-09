import { NextResponse } from 'next/server';

import type { CreateSupplierApplicationRequest } from '@spexcrafters/api-client';

import { invalidRequestBody, readJsonBody } from '@/lib/bff';
import {
  apiErrorResponse,
  isErrorResponse,
  requireApiSessionWithCsrf,
} from '@/lib/org-bff';
import { createServerApiClient } from '@/lib/server-api';

export const runtime = 'nodejs';

/** Create a supplier identity + DRAFT application (requires supplier.create). */
export async function POST(request: Request): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }

  const body = await readJsonBody<CreateSupplierApplicationRequest>(request);
  if (
    !body ||
    typeof body.organizationId !== 'string' ||
    typeof body.originalLocale !== 'string' ||
    typeof body.legalName !== 'string'
  ) {
    return invalidRequestBody();
  }

  try {
    const application = await createServerApiClient(
      session.accessToken,
    ).createSupplierApplication({
      organizationId: body.organizationId,
      originalLocale: body.originalLocale,
      legalName: body.legalName.trim(),
    });
    return NextResponse.json(application, { status: 201 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
