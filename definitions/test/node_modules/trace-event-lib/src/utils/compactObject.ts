/**
 * Mutates the given object by removing all keys with undefined values.
 */
export function compactObject<T>(maybeObject: T): T {
  if (!maybeObject) {
    return maybeObject;
  }

  const object = maybeObject as unknown as Record<string, unknown>;
  for (const key of Object.keys(object)) {
    if (object[key] === undefined) {
      delete object[key];
    }
  }

  return maybeObject;
}
