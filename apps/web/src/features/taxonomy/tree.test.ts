import { describe, expect, it } from 'vitest';

import type { CategoryDetail, CategoryTreeNode } from '@spexcrafters/api-client';

import {
  buildAdminCategoryTree,
  categoryIdByCode,
  countCategories,
  findCategory,
  flattenCategoryTree,
  normalizeCode,
  previewSlug,
} from './tree';

function node(
  code: string,
  children: CategoryTreeNode[] = [],
  active = true,
): CategoryTreeNode {
  return {
    code,
    classification: 'FRAME',
    name: code.toLowerCase(),
    active,
    sortOrder: 0,
    children,
  };
}

const tree: CategoryTreeNode[] = [
  node('FRAME', [node('FRAME_METAL'), node('FRAME_PLASTIC', [node('FRAME_PLASTIC_TR90')])]),
  node('LENS'),
];

describe('flattenCategoryTree', () => {
  it('flattens depth-first with nesting depth preserved', () => {
    const flat = flattenCategoryTree(tree);
    expect(flat.map((c) => `${c.depth}:${c.code}`)).toEqual([
      '0:FRAME',
      '1:FRAME_METAL',
      '1:FRAME_PLASTIC',
      '2:FRAME_PLASTIC_TR90',
      '0:LENS',
    ]);
  });

  it('returns an empty list for an empty tree', () => {
    expect(flattenCategoryTree([])).toEqual([]);
  });
});

describe('countCategories', () => {
  it('counts every node in the tree', () => {
    expect(countCategories(tree)).toBe(5);
  });
});

describe('findCategory', () => {
  it('finds a nested category by code', () => {
    expect(findCategory(tree, 'FRAME_PLASTIC_TR90')?.code).toBe('FRAME_PLASTIC_TR90');
  });

  it('returns null for an unknown code', () => {
    expect(findCategory(tree, 'MISSING')).toBeNull();
  });
});

describe('normalizeCode', () => {
  it('upper-cases and collapses separators to single underscores', () => {
    expect(normalizeCode('Metal frames  2')).toBe('METAL_FRAMES_2');
  });

  it('trims leading and trailing underscores', () => {
    expect(normalizeCode('  frame-metal  ')).toBe('FRAME_METAL');
  });
});

function detail(
  code: string,
  parentCode: string | null,
  depth: number,
  active = true,
): CategoryDetail {
  return {
    id: `id-${code}`,
    code,
    parentCode: parentCode ?? undefined,
    classification: 'FRAME',
    depth,
    path: '/',
    active,
    sortOrder: 0,
    name: code.toLowerCase(),
    version: 0,
  } as CategoryDetail;
}

// Path-ordered (depth-first pre-order) flat list, as the admin API returns it.
const flatAdmin: CategoryDetail[] = [
  detail('FRAME', null, 0),
  detail('FRAME_METAL', 'FRAME', 1, false),
  detail('FRAME_PLASTIC', 'FRAME', 1),
  detail('FRAME_PLASTIC_TR90', 'FRAME_PLASTIC', 2),
  detail('LENS', null, 0),
];

describe('buildAdminCategoryTree', () => {
  it('nests a flat, path-ordered list by parentCode', () => {
    const roots = buildAdminCategoryTree(flatAdmin);
    expect(roots.map((n) => n.code)).toEqual(['FRAME', 'LENS']);
    const frame = roots[0]!;
    expect(frame.children.map((c) => c.code)).toEqual(['FRAME_METAL', 'FRAME_PLASTIC']);
    expect(frame.children[1]!.children.map((c) => c.code)).toEqual(['FRAME_PLASTIC_TR90']);
  });

  it('carries the stable id and active flag onto each node', () => {
    const roots = buildAdminCategoryTree(flatAdmin);
    const metal = roots[0]!.children[0]!;
    expect(metal.id).toBe('id-FRAME_METAL');
    expect(metal.active).toBe(false);
  });

  it('treats a category with a missing parent as a root', () => {
    const orphan = buildAdminCategoryTree([detail('CHILD', 'GHOST', 1)]);
    expect(orphan.map((n) => n.code)).toEqual(['CHILD']);
  });

  it('returns an empty list for an empty input', () => {
    expect(buildAdminCategoryTree([])).toEqual([]);
  });
});

describe('categoryIdByCode', () => {
  it('maps every code to its uuid', () => {
    expect(categoryIdByCode(flatAdmin)).toMatchObject({
      FRAME: 'id-FRAME',
      FRAME_PLASTIC_TR90: 'id-FRAME_PLASTIC_TR90',
      LENS: 'id-LENS',
    });
  });
});

describe('previewSlug', () => {
  it('lower-cases and hyphenates a display name', () => {
    expect(previewSlug('Metal Frames')).toBe('metal-frames');
  });

  it('drops diacritics and collapses runs', () => {
    expect(previewSlug('Lunettes Élégantes')).toBe('lunettes-elegantes');
  });

  it('trims edge hyphens', () => {
    expect(previewSlug('  Frame — Metal  ')).toBe('frame-metal');
  });
});
