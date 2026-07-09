'use client';

import { useState } from 'react';

import { Button } from '@spexcrafters/ui';

import type { SupportedLocale } from '@/i18n/locales';
import { sendJson } from '@/lib/csrf-client';

interface LogoutButtonProps {
  locale: SupportedLocale;
  label: string;
}

export function LogoutButton({ locale, label }: LogoutButtonProps) {
  const [busy, setBusy] = useState(false);

  async function logout() {
    if (busy) {
      return;
    }
    setBusy(true);
    try {
      await sendJson('/api/auth/logout', 'POST');
    } finally {
      // Full navigation so the server re-renders without the session cookie.
      window.location.assign(`/${locale}`);
    }
  }

  return (
    <Button variant="quiet" size="sm" type="button" loading={busy} onClick={logout}>
      {label}
    </Button>
  );
}
