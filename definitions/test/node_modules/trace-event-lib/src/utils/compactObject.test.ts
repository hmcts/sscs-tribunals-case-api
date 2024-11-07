import { compactObject } from './compactObject';

describe('compactObject', () => {
  it('should mutate the original object', () => {
    const input = { b: 2, c: 3 };
    const result = compactObject(input);
    expect(result).toBe(input);
  });

  it('should omit undefined properties', () => {
    const input = { a: undefined, b: 2, c: 3, d: undefined };
    const result = compactObject(input);

    expect(result).toEqual({ b: 2, c: 3 });
  });

  it('should have a fallback for non-objects', () => {
    const result = compactObject(undefined as any);
    expect(result).toBe(undefined);
  });
});
