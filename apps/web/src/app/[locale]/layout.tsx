import '@spexcrafters/design-tokens/css';
import '../globals.css';

import type { Metadata } from 'next';
import type { ReactNode } from 'react';

import {
  defaultLocale,
  getDictionary,
  isLocale,
  locales,
  type Locale,
} from '@/lib/i18n';

interface LocaleParams {
  params: Promise<{ locale: string }>;
}

export function generateStaticParams(): Array<{ locale: Locale }> {
  return locales.map((locale) => ({ locale }));
}

export async function generateMetadata({ params }: LocaleParams): Promise<Metadata> {
  const { locale } = await params;
  const dict = getDictionary(locale);
  return {
    metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3000'),
    title: {
      template: `%s · ${dict.common.appName}`,
      default: dict.common.appName,
    },
    description: dict.home.metaDescription,
  };
}

export default async function LocaleLayout({
  children,
  params,
}: LocaleParams & { children: ReactNode }) {
  const { locale: raw } = await params;
  // The middleware guarantees a valid locale segment; fall back defensively
  // so a direct render can never produce an unlabelled document.
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;

  return (
    <html lang={locale} data-theme="light">
      <body>{children}</body>
    </html>
  );
}
