import { NextResponse } from 'next/server';

import type { ResendVerificationRequest } from '@spexcrafters/api-client';

import {
  apiFetch,
  invalidRequestBody,
  problemToResponse,
  readJsonBody,
  upstreamUnavailable,
} from '@/lib/bff';

export const runtime = 'nodejs';

export async function POST(request: Request): Promise<NextResponse> {
  const body = await readJsonBody<ResendVerificationRequest>(request);
  if (!body || typeof body.email !== 'string' || body.email.length === 0) {
    return invalidRequestBody();
  }

  let upstream: Response;
  try {
    upstream = await apiFetch('/auth/resend-verification', { email: body.email });
  } catch {
    return upstreamUnavailable();
  }

  if (upstream.status === 202) {
    return new NextResponse(null, { status: 202 });
  }

  return problemToResponse(upstream);
}
