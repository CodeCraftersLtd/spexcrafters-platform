import { NextResponse } from 'next/server';

import type { RegisterRequest, RegisterResponse } from '@spexcrafters/api-client';

import {
  apiFetch,
  invalidRequestBody,
  problemToResponse,
  readJsonBody,
  upstreamUnavailable,
} from '@/lib/bff';
import { enforceUnauthenticatedOrigin } from '@/lib/csrf';

export const runtime = 'nodejs';

export async function POST(request: Request): Promise<NextResponse> {
  const csrfError = enforceUnauthenticatedOrigin(request);
  if (csrfError) {
    return csrfError;
  }

  const body = await readJsonBody<RegisterRequest>(request);
  if (!body) {
    return invalidRequestBody();
  }

  let upstream: Response;
  try {
    upstream = await apiFetch('/auth/register', body);
  } catch {
    return upstreamUnavailable();
  }

  if (upstream.status === 201) {
    const data = (await upstream.json()) as RegisterResponse;
    return NextResponse.json(data, { status: 201 });
  }

  return problemToResponse(upstream);
}
