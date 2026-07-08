import { NextResponse } from 'next/server';

import type { VerifyEmailRequest } from '@spexcrafters/api-client';

import {
  apiFetch,
  invalidRequestBody,
  problemToResponse,
  readJsonBody,
  upstreamUnavailable,
} from '@/lib/bff';

export const runtime = 'nodejs';

export async function POST(request: Request): Promise<NextResponse> {
  const body = await readJsonBody<VerifyEmailRequest>(request);
  if (!body || typeof body.token !== 'string' || body.token.length === 0) {
    return invalidRequestBody();
  }

  let upstream: Response;
  try {
    upstream = await apiFetch('/auth/verify-email', { token: body.token });
  } catch {
    return upstreamUnavailable();
  }

  if (upstream.status === 204) {
    return new NextResponse(null, { status: 204 });
  }

  return problemToResponse(upstream);
}
