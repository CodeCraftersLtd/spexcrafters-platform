import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NextIntlClientProvider } from 'next-intl';
import { forwardRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import auth from '../../../messages/en/auth.json';

// Lightweight stand-ins for the workspace UI package so the form's behavior
// (labels, validation wiring, error surfacing) is tested in isolation.
vi.mock('@spexcrafters/ui', () => {
  interface MockButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: string;
    size?: string;
    loading?: boolean;
    children?: ReactNode;
  }
  function Button({ loading, children, ...rest }: MockButtonProps) {
    // variant/size are design-system props consumed by the real Button; the
    // mock ignores them (they pass through in ...rest as harmless lowercase
    // DOM attributes).
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

vi.mock('next/link', () => ({
  default: ({ href, children }: { href: string; children: ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}));

import { LoginForm } from './LoginForm';

const copy = auth.login;
const validation = auth.validation;
const serverErrors = auth.serverErrors;

function renderLoginForm() {
  return render(
    <NextIntlClientProvider locale="en" messages={{ auth }}>
      <LoginForm locale="en" />
    </NextIntlClientProvider>,
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('LoginForm', () => {
  it('renders labelled email and password fields and a submit button', () => {
    renderLoginForm();
    expect(screen.getByLabelText(copy.emailLabel)).toBeInTheDocument();
    expect(screen.getByLabelText(copy.passwordLabel)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: copy.submit })).toBeInTheDocument();
  });

  it('shows dictionary validation errors when submitted empty', async () => {
    const fetchSpy = vi.fn();
    vi.stubGlobal('fetch', fetchSpy);
    const user = userEvent.setup();
    renderLoginForm();

    await user.click(screen.getByRole('button', { name: copy.submit }));

    expect(await screen.findByText(validation.emailRequired)).toBeInTheDocument();
    expect(await screen.findByText(validation.passwordRequired)).toBeInTheDocument();
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it('shows the invalid-email message for a malformed address', async () => {
    vi.stubGlobal('fetch', vi.fn());
    const user = userEvent.setup();
    renderLoginForm();

    await user.type(screen.getByLabelText(copy.emailLabel), 'not-an-email');
    await user.type(screen.getByLabelText(copy.passwordLabel), 'some-password');
    await user.click(screen.getByRole('button', { name: copy.submit }));

    expect(await screen.findByText(validation.emailInvalid)).toBeInTheDocument();
  });

  it('surfaces an authentication failure from the BFF as an alert', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            error: { code: 'authentication-failed', message: '' },
          }),
          { status: 401, headers: { 'content-type': 'application/json' } },
        ),
      ),
    );
    const user = userEvent.setup();
    renderLoginForm();

    await user.type(screen.getByLabelText(copy.emailLabel), 'ada@example.com');
    await user.type(screen.getByLabelText(copy.passwordLabel), 'wrong-password');
    await user.click(screen.getByRole('button', { name: copy.submit }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(serverErrors['authentication-failed']);
  });
});
