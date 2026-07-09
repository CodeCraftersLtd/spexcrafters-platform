import { getMessages } from 'next-intl/server';

import type { Namespace } from './messages';

/**
 * Server helper: return only the requested namespaces from the active locale's
 * messages, for handing to a NextIntlClientProvider. Client islands must never
 * receive the full message set (ADR-019 — bundle discipline).
 */
export async function pickMessages(
  namespaces: readonly Namespace[],
): Promise<Record<string, unknown>> {
  const all = (await getMessages()) as Record<string, unknown>;
  const picked: Record<string, unknown> = {};
  for (const namespace of namespaces) {
    picked[namespace] = all[namespace];
  }
  return picked;
}
