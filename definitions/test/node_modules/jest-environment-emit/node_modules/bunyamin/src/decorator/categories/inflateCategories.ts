export function inflateCategories(cat: unknown): string[] {
  if (!cat) {
    return [];
  }

  if (Array.isArray(cat)) {
    return cat;
  }

  return String(cat).split(',');
}
