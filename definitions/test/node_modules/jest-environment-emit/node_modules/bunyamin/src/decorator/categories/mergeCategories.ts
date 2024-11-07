import { inflateCategories } from './inflateCategories';

export function mergeCategories(left: string[] | undefined, right: unknown): string[] | undefined {
  if (!left || !right) {
    if (left) return left;
    if (right) return inflateCategories(right);
    return undefined;
  }

  const iright = inflateCategories(right);
  const categories = left ? [...left, ...iright] : iright;
  const uniqueCategories = new Set(categories);
  return [...uniqueCategories.values()];
}
