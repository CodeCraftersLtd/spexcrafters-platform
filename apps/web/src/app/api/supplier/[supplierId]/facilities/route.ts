import { NextResponse } from 'next/server';

import type { AddFacilityRequest } from '@spexcrafters/api-client';

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

/** Add a facility with its original-language name/description (supplier.update). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { supplierId } = await context.params;
  const body = await readJsonBody<AddFacilityRequest>(request);
  if (
    !body ||
    typeof body.facilityTypeCode !== 'string' ||
    typeof body.country !== 'string' ||
    typeof body.addressPrivacy !== 'string' ||
    typeof body.ownership !== 'string'
  ) {
    return invalidRequestBody();
  }
  try {
    const facility = await createServerApiClient(
      session.accessToken,
    ).addSupplierFacility(supplierId, body);
    return NextResponse.json(facility, { status: 201 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
