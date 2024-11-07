export function assertString(
  value: string | number | symbol,
  name: string,
): asserts value is string {
  assertType('string', value, name);
}

// eslint-disable-next-line @typescript-eslint/ban-types
export function assertFunction(value: Function, name: string): asserts value is Function {
  assertType('function', value, name);
}

export function assertNumber(value: number, name: string): asserts value is number {
  assertType('number', value, name);
}

function assertType(type: 'string' | 'function' | 'number', value: unknown, name: string) {
  if (typeof value !== type) {
    throw new TypeError(`${name} must be a ${type}`);
  }
}
