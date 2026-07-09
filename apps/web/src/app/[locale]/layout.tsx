import '@spexcrafters/design-tokens/css';
import '../globals.css';
import '../fonts.css';

import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { NextIntlClientProvider } from 'next-intl';
import { getTranslations, setRequestLocale } from 'next-intl/server';
import type { ReactNode } from 'react';

import { dirOf, isSupportedLocale, LOCALES, type SupportedLocale } from '@/i18n/locales';
import { pickMessages } from '@/i18n/pick';

interface LocaleParams {
  params: Promise<{ locale: string }>;
}

export function generateStaticParams(): Array<{ locale: SupportedLocale }> {
  return LOCALES.map((locale) => ({ locale }));
}

export async function generateMetadata({ params }: LocaleParams): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'seo' });
  return {
    metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3000'),
    title: {
      template: `%s · ${t('siteName')}`,
      default: t('siteName'),
    },
    description: t('defaultDescription'),
    alternates: {
      languages: Object.fromEntries(LOCALES.map((l) => [l, `/${l}`])),
    },
    openGraph: {
      siteName: t('siteName'),
      locale,
      type: 'website',
    },
  };
}

export default async function LocaleLayout({
  children,
  params,
}: LocaleParams & { children: ReactNode }) {
  const { locale: raw } = await params;
  if (!isSupportedLocale(raw)) {
    // An unsupported segment reached the layout (middleware normally rewrites);
    // render the localized 404 rather than an unlabelled document.
    notFound();
  }
  const locale: SupportedLocale = raw;
  // Enables static rendering of the locale subtree (next-intl).
  setRequestLocale(locale);

  // Shared chrome namespaces handed to every client island in the subtree.
  // Feature islands (auth/org forms) receive their own namespace via a nested
  // provider at the page level — never the full message set.
  const messages = await pickMessages([
    'common',
    'navigation',
    'accessibility',
    'errors',
  ]);

  return (
    <html lang={locale} dir={dirOf(locale)} data-theme="light">
      <body>
        <NextIntlClientProvider locale={locale} messages={messages}>
          {children}
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
