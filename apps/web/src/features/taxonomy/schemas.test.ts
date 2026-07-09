import { describe, expect, it } from 'vitest';

import {
  addEnumerationValueSchema,
  createAttributeSchema,
  createBrandSchema,
  createCategorySchema,
  createCertificationSchema,
  createEnumerationSchema,
  createUnitSchema,
  specificationTemplateSchema,
  translationUpsertSchema,
} from './schemas';

// Echo translate function so assertions can match on the key.
const t = (key: string) => key;

describe('createCategorySchema', () => {
  const schema = createCategorySchema(t);

  it('accepts a valid root category', () => {
    const result = schema.safeParse({
      code: 'FRAME_METAL',
      parentCode: '',
      classification: 'FRAME',
      originalLocale: 'en',
      name: 'Metal frames',
      sortOrder: '10',
    });
    expect(result.success).toBe(true);
  });

  it('rejects a lower-case / punctuated code', () => {
    const result = schema.safeParse({
      code: 'frame-metal',
      classification: 'FRAME',
      originalLocale: 'en',
      name: 'Metal frames',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe('codeInvalid');
    }
  });

  it('requires a display name', () => {
    const result = schema.safeParse({
      code: 'FRAME',
      classification: 'FRAME',
      originalLocale: 'en',
      name: '  ',
    });
    expect(result.success).toBe(false);
  });

  it('rejects an unknown classification', () => {
    const result = schema.safeParse({
      code: 'FRAME',
      classification: 'NOPE',
      originalLocale: 'en',
      name: 'Frames',
    });
    expect(result.success).toBe(false);
  });

  it('rejects a non-numeric sort order', () => {
    const result = schema.safeParse({
      code: 'FRAME',
      classification: 'FRAME',
      originalLocale: 'en',
      name: 'Frames',
      sortOrder: 'x',
    });
    expect(result.success).toBe(false);
  });
});

describe('createAttributeSchema', () => {
  const schema = createAttributeSchema(t);

  it('accepts a valid attribute', () => {
    const result = schema.safeParse({
      code: 'LENS_INDEX',
      dataType: 'DECIMAL',
      unitCode: '',
      enumerationCode: '',
      originalLocale: 'en',
      name: 'Lens index',
      description: '',
      searchable: true,
      filterable: true,
      visible: true,
    });
    expect(result.success).toBe(true);
  });

  it('rejects an unknown data type', () => {
    const result = schema.safeParse({
      code: 'LENS_INDEX',
      dataType: 'FLOATY',
      originalLocale: 'en',
      name: 'Lens index',
      searchable: false,
      filterable: false,
      visible: true,
    });
    expect(result.success).toBe(false);
  });
});

describe('createEnumerationSchema + addEnumerationValueSchema', () => {
  it('accepts an enumeration code', () => {
    expect(createEnumerationSchema(t).safeParse({ code: 'LENS_MATERIAL' }).success).toBe(
      true,
    );
  });

  it('requires a value label', () => {
    const result = addEnumerationValueSchema(t).safeParse({
      code: 'CR39',
      label: '',
      originalLocale: 'en',
      sortOrder: '',
      description: '',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.message === 'labelRequired')).toBe(true);
    }
  });
});

describe('createBrandSchema', () => {
  const schema = createBrandSchema(t);

  it('accepts a valid brand and upper-cases the country', () => {
    const result = schema.safeParse({
      code: 'ACME_OPTICS',
      brandType: 'FRAME',
      canonicalName: 'Acme Optics',
      displayName: '',
      ownerCompany: '',
      manufacturer: '',
      countryCode: 'cn',
      website: '',
      originalLocale: 'en',
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.countryCode).toBe('CN');
    }
  });

  it('rejects an invalid website URL', () => {
    const result = schema.safeParse({
      code: 'ACME_OPTICS',
      brandType: 'FRAME',
      canonicalName: 'Acme Optics',
      website: 'not-a-url',
      originalLocale: 'en',
    });
    expect(result.success).toBe(false);
  });
});

describe('createCertificationSchema', () => {
  it('accepts a certification with an optional category', () => {
    const result = createCertificationSchema(t).safeParse({
      code: 'ISO_9001',
      category: 'QUALITY',
      countryScope: '',
      validityMonths: '36',
      originalLocale: 'en',
      name: 'ISO 9001',
      description: '',
    });
    expect(result.success).toBe(true);
  });
});

describe('createUnitSchema', () => {
  const schema = createUnitSchema(t);

  it('accepts a short symbol code', () => {
    const result = schema.safeParse({
      code: 'mm',
      family: 'LENGTH',
      baseUnitCode: '',
      factorToBase: '0.001',
      originalLocale: 'en',
      displayName: 'Millimetre',
    });
    expect(result.success).toBe(true);
  });

  it('rejects a non-numeric factor', () => {
    const result = schema.safeParse({
      code: 'mm',
      family: 'LENGTH',
      factorToBase: 'abc',
      originalLocale: 'en',
      displayName: 'Millimetre',
    });
    expect(result.success).toBe(false);
  });
});

describe('specificationTemplateSchema', () => {
  const schema = specificationTemplateSchema(t);

  it('accepts a template with at least one attribute row', () => {
    const result = schema.safeParse({
      categoryId: 'cat-1',
      code: 'FRAME_TEMPLATE',
      attributes: [
        { attributeCode: 'LENS_INDEX', required: true, sortOrder: '0', defaultValue: '' },
      ],
    });
    expect(result.success).toBe(true);
  });

  it('rejects an empty attribute list', () => {
    const result = schema.safeParse({
      categoryId: 'cat-1',
      code: 'FRAME_TEMPLATE',
      attributes: [],
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.message === 'atLeastOneAttribute')).toBe(
        true,
      );
    }
  });
});

describe('translationUpsertSchema', () => {
  it('accepts a human translation', () => {
    const result = translationUpsertSchema(t).safeParse({
      locale: 'es',
      name: 'Monturas metálicas',
      description: '',
      source: 'HUMAN',
    });
    expect(result.success).toBe(true);
  });

  it('requires a translated name', () => {
    const result = translationUpsertSchema(t).safeParse({
      locale: 'es',
      name: '',
      source: 'HUMAN',
    });
    expect(result.success).toBe(false);
  });
});
