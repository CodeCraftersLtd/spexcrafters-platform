import { render, screen } from '@testing-library/react';
import { forwardRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';

import type { Capability, InvitationResponse } from '@spexcrafters/api-client';

import en from '../../../messages/en.json';

// Lightweight stand-ins for the workspace UI package so capability gating is
// tested in isolation (same approach as LoginForm.test).
vi.mock('@spexcrafters/ui', () => {
  interface MockButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: string;
    size?: string;
    loading?: boolean;
    children?: ReactNode;
  }
  function Button({ variant, size, loading, children, ...rest }: MockButtonProps) {
    return (
      <button aria-busy={loading || undefined} {...rest}>
        {children}
      </button>
    );
  }

  interface MockInputProps extends InputHTMLAttributes<HTMLInputElement> {
    invalid?: boolean;
  }
  const Input = forwardRef<HTMLInputElement, MockInputProps>(function Input(
    { invalid, ...rest },
    ref,
  ) {
    return <input ref={ref} aria-invalid={invalid || undefined} {...rest} />;
  });

  interface MockFormFieldProps {
    label: string;
    htmlFor: string;
    hint?: string;
    error?: string;
    children: ReactNode;
  }
  function FormField({ label, htmlFor, hint, error, children }: MockFormFieldProps) {
    return (
      <div>
        <label htmlFor={htmlFor}>{label}</label>
        {children}
        {hint ? <p>{hint}</p> : null}
        {error ? <p id={`${htmlFor}-error`}>{error}</p> : null}
      </div>
    );
  }

  interface MockAlertProps {
    tone: string;
    title?: string;
    children?: ReactNode;
  }
  function Alert({ title, children }: MockAlertProps) {
    return (
      <div role="alert">
        {title ? <strong>{title}</strong> : null}
        {children}
      </div>
    );
  }

  return { Button, Input, FormField, Alert };
});

vi.mock('next/navigation', () => ({
  useRouter: () => ({ refresh: vi.fn(), push: vi.fn() }),
}));

import { InvitationsSection } from './InvitationsSection';

const copy = en.organizations.workspace.invitations;
const roles = en.organizations.roles;
const validation = en.organizations.validation;
const serverErrors = en.organizations.serverErrors;

const READ_ONLY: Capability[] = ['organization.read', 'organization.members.read'];
const ADMIN_CAPS: Capability[] = [
  ...READ_ONLY,
  'organization.update',
  'organization.members.invite',
  'organization.members.remove',
];
const OWNER_CAPS: Capability[] = [...ADMIN_CAPS, 'organization.roles.manage'];

const PENDING_INVITATION: InvitationResponse = {
  id: '018f63f1-0000-7000-8000-000000000001',
  email: 'invitee@example.com',
  role: 'MEMBER',
  status: 'PENDING',
  expiresAt: '2026-07-16T00:00:00Z',
  createdAt: '2026-07-09T00:00:00Z',
};

function renderSection(
  callerCapabilities: Capability[],
  invitations: InvitationResponse[] = [],
) {
  return render(
    <InvitationsSection
      locale="en"
      organizationId="018f63f1-0000-7000-8000-00000000000a"
      invitations={invitations}
      callerCapabilities={callerCapabilities}
      copy={copy}
      roles={roles}
      validation={validation}
      serverErrors={serverErrors}
    />,
  );
}

describe('InvitationsSection capability gating', () => {
  it('renders the invite form when the caller holds organization.members.invite', () => {
    renderSection(ADMIN_CAPS);
    expect(screen.getByRole('form', { name: copy.inviteTitle })).toBeInTheDocument();
    expect(screen.getByLabelText(copy.emailLabel)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: copy.submit })).toBeInTheDocument();
  });

  it('hides the invite form entirely without organization.members.invite', () => {
    renderSection(READ_ONLY);
    expect(screen.queryByRole('form', { name: copy.inviteTitle })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(copy.emailLabel)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: copy.submit })).not.toBeInTheDocument();
  });

  it('offers MEMBER only to inviters without organization.roles.manage', () => {
    renderSection(ADMIN_CAPS);
    const options = screen
      .getAllByRole('option')
      .map((option) => (option as HTMLOptionElement).value);
    expect(options).toEqual(['MEMBER']);
  });

  it('offers MEMBER and ADMIN to holders of organization.roles.manage', () => {
    renderSection(OWNER_CAPS);
    const options = screen
      .getAllByRole('option')
      .map((option) => (option as HTMLOptionElement).value);
    expect(options).toEqual(['MEMBER', 'ADMIN']);
  });

  it('hides the revoke control for callers who cannot invite', () => {
    renderSection(READ_ONLY, [PENDING_INVITATION]);
    expect(screen.getByText(PENDING_INVITATION.email)).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /revoke/i }),
    ).not.toBeInTheDocument();
  });

  it('shows the revoke control on pending invitations for inviters', () => {
    renderSection(ADMIN_CAPS, [PENDING_INVITATION]);
    expect(
      screen.getByRole('button', {
        name: `Revoke the invitation for ${PENDING_INVITATION.email}`,
      }),
    ).toBeInTheDocument();
  });
});
