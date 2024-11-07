export function isActionable<T>(value: T | (() => T)): value is () => T {
  return typeof value === 'function';
}
