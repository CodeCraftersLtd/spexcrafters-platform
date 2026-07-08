'use client';

import { useState } from 'react';

import { Button } from '@spexcrafters/ui';

import type { Locale } from '@/lib/i18n';

interface LogoutButtonProps {
  locale: Locale;
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
      await fetch('/api/auth/logout', { method: 'POST' });
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
