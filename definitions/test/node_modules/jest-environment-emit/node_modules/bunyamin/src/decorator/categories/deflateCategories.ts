export function deflateCategories(cat: unknown): string | undefined {
  if (!cat) {
    return undefined;
  }

  if (Array.isArray(cat)) {
    return cat.filter(Boolean).join(',');
  }

  return String(cat);
}
