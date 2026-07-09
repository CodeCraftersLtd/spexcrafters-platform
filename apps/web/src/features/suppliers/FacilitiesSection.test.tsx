import { render, screen } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import {
  forwardRef,
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type ReactNode,
} from 'react';
import { describe, expect, it, vi } from 'vitest';

import suppliers from '../../../messages/en/suppliers.json';
import taxonomy from '../../../messages/en/taxonomy.json';
import errors from '../../../messages/en/errors.json';

// Lightweight stand-ins for the design-system package so capability gating is
// tested in isolation (same approach as InvitationsSection.test).
vi.mock('@spexcrafters/ui', () => {
  function Button({
    loading,
    children,
    ...rest
  }: ButtonHTMLAttributes<HTMLButtonElement> & { loading?: boolean; variant?: string; size?: string }) {
    return (
      <button aria-busy={loading || undefined} {...rest}>
        {children}
      </button>
    );
  }
  const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }>(
    function Input({ invalid, ...rest }, ref) {
      return <input ref={ref} aria-invalid={invalid || undefined} {...rest} />;
    },
  );
  function FormField({
    label,
    htmlFor,
    error,
    children,
  }: {
    label: string;
    htmlFor: string;
    error?: string;
    children: ReactNode;
  }) {
    return (
      <div>
        <label htmlFor={htmlFor}>{label}</label>
        {children}
        {error ? <p>{error}</p> : null}
      </div>
    );
  }
  function Alert({ children }: { tone: string; children?: ReactNode }) {
    return <div role="alert">{children}</div>;
  }
  return { Button, Input, FormField, Alert };
});

vi.mock('next/navigation', () => ({
  useRouter: () => ({ refresh: vi.fn(), push: vi.fn() }),
}));

import { FacilitiesSection } from './FacilitiesSection';

const copy = suppliers.facilities;

function renderSection(canUpdate: boolean) {
  return render(
    <NextIntlClientProvider locale="en" messages={{ suppliers, taxonomy, errors }}>
      <FacilitiesSection supplierId="s-1" facilities={[]} canUpdate={canUpdate} />
    </NextIntlClientProvider>,
  );
}

describe('FacilitiesSection capability gating', () => {
  it('renders the add-facility form when the caller can update', () => {
    renderSection(true);
    expect(screen.getByRole('form', { name: copy.addTitle })).toBeInTheDocument();
    expect(screen.getByLabelText(copy.countryLabel)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: copy.add })).toBeInTheDocument();
  });

  it('hides the add-facility form entirely without update capability', () => {
    renderSection(false);
    expect(screen.queryByRole('form', { name: copy.addTitle })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(copy.countryLabel)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: copy.add })).not.toBeInTheDocument();
  });

  it('shows the empty state in both cases', () => {
    renderSection(false);
    expect(screen.getByText(copy.empty)).toBeInTheDocument();
  });
});
