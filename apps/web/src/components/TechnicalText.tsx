import type { ReactNode } from 'react';

interface TechnicalTextProps {
  children: ReactNode;
  className?: string;
}

/**
 * Bidi-isolates an LTR technical run (model code, SKU, GTIN, Rx/dioptre value,
 * ISO code, measurement) so it does not reorder inside RTL prose (ADR-021,
 * Class E content). Renders `<bdi dir="ltr">` — direction is forced LTR and the
 * run is isolated from the surrounding paragraph's bidi context. Never mirrored.
 */
export function TechnicalText({ children, className }: TechnicalTextProps) {
  return (
    <bdi dir="ltr" className={className}>
      {children}
    </bdi>
  );
}
