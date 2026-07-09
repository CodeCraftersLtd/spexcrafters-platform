import type { CategoryTreeNode } from '@spexcrafters/api-client';

/**
 * Pure helpers for the category tree (framework-agnostic; unit-tested). The
 * public read API already returns a nested `CategoryTreeNode[]`; these flatten
 * it for the parent/category pickers and derive slug/code previews for the
 * create forms.
 */

export interface FlatCategory {
  code: string;
  name: string;
  /** 0 for a root category; +1 per level of nesting. */
  depth: number;
  active: boolean;
}

/**
 * Depth-first pre-order flatten, preserving sibling order. Each node carries
 * its nesting depth so a `<select>` can indent children under their parent.
 */
export function flattenCategoryTree(
  nodes: readonly CategoryTreeNode[],
  depth = 0,
): FlatCategory[] {
  const out: FlatCategory[] = [];
  for (const node of nodes) {
    out.push({ code: node.code, name: node.name, depth, active: node.active });
    if (node.children.length > 0) {
      out.push(...flattenCategoryTree(node.children, depth + 1));
    }
  }
  return out;
}

/** Total number of categories in a (possibly nested) tree. */
export function countCategories(nodes: readonly CategoryTreeNode[]): number {
  return nodes.reduce(
    (total, node) => total + 1 + countCategories(node.children),
    0,
  );
}

/**
 * Find a node by code anywhere in the tree (depth-first), or null. Used to
 * confirm a freshly created child landed under its intended parent.
 */
export function findCategory(
  nodes: readonly CategoryTreeNode[],
  code: string,
): CategoryTreeNode | null {
  for (const node of nodes) {
    if (node.code === code) {
      return node;
    }
    const nested = findCategory(node.children, code);
    if (nested) {
      return nested;
    }
  }
  return null;
}

/**
 * Normalize free-typed input into a candidate code: upper-case, non-identifier
 * runs collapsed to a single underscore, edge underscores trimmed. Advisory
 * only — the server owns code identity — but keeps the create form forgiving.
 */
export function normalizeCode(input: string): string {
  return input
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
}

/**
 * Preview the SEO slug the registry would derive from a display name: lower-case,
 * diacritics dropped where representable, non-alphanumerics collapsed to single
 * hyphens, edges trimmed. Preview only — the server authors the canonical slug.
 */
export function previewSlug(name: string): string {
  // NFKD splits accented letters into base + combining mark (U+0300–U+036F);
  // strip the marks so the base letter survives the alphanumeric collapse.
  return name
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}
