import type { CategoryDetail, CategoryTreeNode } from '@spexcrafters/api-client';

/**
 * Pure helpers for the category tree (framework-agnostic; unit-tested). The
 * public read API already returns a nested `CategoryTreeNode[]`; these flatten
 * it for the parent/category pickers and derive slug/code previews for the
 * create forms.
 */

/**
 * A category node for the admin dashboard, nested from the FLAT
 * `listAdminCategories` read. Unlike the public `CategoryTreeNode`, this carries
 * the stable `id` (so translation actions target the uuid directly) and includes
 * inactive categories.
 */
export interface AdminCategoryNode {
  id: string;
  code: string;
  parentCode: string | null;
  classification: CategoryDetail['classification'];
  name: string;
  active: boolean;
  depth: number;
  sortOrder: number;
  children: AdminCategoryNode[];
}

/**
 * Nest the flat, path-ordered `listAdminCategories` result by `parentCode`.
 * Sibling order follows input order (the API returns categories ordered by
 * materialized path, i.e. depth-first pre-order). A category whose `parentCode`
 * is absent from the input (or null) becomes a root, so the whole set is always
 * reachable even if an ancestor is missing.
 */
export function buildAdminCategoryTree(
  categories: readonly CategoryDetail[],
): AdminCategoryNode[] {
  const byCode = new Map<string, AdminCategoryNode>();
  for (const category of categories) {
    byCode.set(category.code, {
      id: category.id,
      code: category.code,
      parentCode: category.parentCode ?? null,
      classification: category.classification,
      name: category.name,
      active: category.active,
      depth: category.depth,
      sortOrder: category.sortOrder,
      children: [],
    });
  }
  const roots: AdminCategoryNode[] = [];
  for (const category of categories) {
    const node = byCode.get(category.code)!;
    const parent = node.parentCode ? byCode.get(node.parentCode) : undefined;
    if (parent) {
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  }
  return roots;
}

/** code → uuid map built from the flat `listAdminCategories` read. */
export function categoryIdByCode(
  categories: readonly CategoryDetail[],
): Record<string, string> {
  const map: Record<string, string> = {};
  for (const category of categories) {
    map[category.code] = category.id;
  }
  return map;
}

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
