import { NextResponse } from 'next/server';

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

/** Approve a category translation for a locale (platform-staff: TAXONOMY_WRITE). */
export async function POST(
  request: Request,
  context: RouteContext,
): Promise<NextResponse> {
  const session = await requireApiSessionWithCsrf(request);
  if (isErrorResponse(session)) {
    return session;
  }
  const { id, locale } = await context.params;
  try {
    const view = await createServerApiClient(
      session.accessToken,
    ).approveCategoryTranslation(id, locale);
    return NextResponse.json(view, { status: 200 });
  } catch (error) {
    return apiErrorResponse(error);
  }
}
