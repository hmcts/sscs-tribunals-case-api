export function flow<T1, T2, R>(f: (x: T1) => T2, g: (x: T2) => R): (x: T1) => R {
  return (x: T1) => g(f(x));
}
