import { NextResponse } from 'next/server';

import type { LoginRequest, TokenResponse } from '@spexcrafters/api-client';

import {
  apiFetch,
  invalidRequestBody,
  problemToResponse,
  readJsonBody,
  upstreamUnavailable,
} from '@/lib/bff';
import { createSession } from '@/lib/session';

export const runtime = 'nodejs';

export async function POST(request: Request): Promise<NextResponse> {
  const body = await readJsonBody<LoginRequest>(request);
  if (!body) {
    return invalidRequestBody();
  }

  let upstream: Response;
  try {
    upstream = await apiFetch('/auth/login', body);
  } catch {
    return upstreamUnavailable();
  }

  if (upstream.status === 200) {
    const tokens = (await upstream.json()) as TokenResponse;
    const session = await createSession(tokens);
    return NextResponse.json({ user: session.user }, { status: 200 });
  }

  return problemToResponse(upstream);
}
