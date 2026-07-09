import { describe, expect, it } from 'vitest';

import type { CategoryTreeNode } from '@spexcrafters/api-client';

import {
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
