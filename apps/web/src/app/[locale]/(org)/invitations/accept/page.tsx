import type { Metadata } from 'next';
import { redirect } from 'next/navigation';

import { AcceptInvitation } from '@/features/organizations/AcceptInvitation';
import { defaultLocale, getDictionary, isLocale, type Locale } from '@/lib/i18n';
import { getSession } from '@/lib/session';

import styles from './page.module.css';

interface AcceptInvitationPageProps {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ token?: string | string[] }>;
}

export async function generateMetadata({
  params,
}: AcceptInvitationPageProps): Promise<Metadata> {
  const { locale } = await params;
  return {
    title: getDictionary(locale).invitations.accept.metaTitle,
    robots: { index: false, follow: false },
  };
}

export default async function AcceptInvitationPage({
  params,
  searchParams,
}: AcceptInvitationPageProps) {
  const [{ locale: raw }, { token }] = await Promise.all([params, searchParams]);
  const locale: Locale = isLocale(raw) ? raw : defaultLocale;
  const dict = getDictionary(locale);
  const copy = dict.invitations.accept;
  const tokenValue = Array.isArray(token) ? token[0] : token;

  // Server-side enforcement — the middleware cookie check is UX only. The
  // returnTo keeps the single-use token so acceptance resumes after sign-in.
  const session = await getSession();
  if (!session) {
    const returnTo = `/${locale}/invitations/accept${
      tokenValue ? `?token=${encodeURIComponent(tokenValue)}` : ''
    }`;
    redirect(`/${locale}/auth/login?returnTo=${encodeURIComponent(returnTo)}`);
  }

  return (
    <div className={styles.wrapper}>
      <section className={styles.card}>
        <h1 className={styles.title}>{copy.title}</h1>
        <AcceptInvitation
          locale={locale}
          token={tokenValue && tokenValue.length > 0 ? tokenValue : null}
          copy={copy}
        />
      </section>
    </div>
  );
}
