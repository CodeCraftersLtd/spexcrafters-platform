import '@testing-library/jest-dom/vitest';

import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// RTL auto-cleanup only engages when test globals are exposed; vitest runs
// without `globals: true` here, so unmount rendered trees explicitly.
afterEach(() => {
  cleanup();
});
