/***
 * Rewritten version of {@link https://github.com/MatrixAI/js-permaproxy}
 * The original code was under Apache-2.0 license, as of 11.05.2019.
 * Differences from the original:
 *
 * 1. Instead of an object reference and property name, uses getter function.
 * 2. Rewritten in arrow functions
 */
function _permaproxy(directGetter, fallbackTarget) {
  const getter = () => directGetter() || fallbackTarget;

  return new Proxy(fallbackTarget, {
    getPrototypeOf: (_) => Reflect.getPrototypeOf(getter()),
    setPrototypeOf: (_, prototype) => Reflect.setPrototypeOf(getter(), prototype),
    isExtensible: (_) => Reflect.isExtensible(getter()),
    preventExtensions: (_) => Reflect.preventExtensions(getter()),
    getOwnPropertyDescriptor: (_, property) => Reflect.getOwnPropertyDescriptor(getter(), property),
    defineProperty: (_, property, descriptor) => Reflect.defineProperty(getter(), property, descriptor),
    has: (_, property) => Reflect.has(getter(), property),
    get: (_, property) => {
      const target = getter();
      const value = Reflect.get(target, property);

      return (typeof value === 'function') ? value.bind(target) : value;
    },
    set: (_, property, value) => Reflect.set(getter(), property, value),
    deleteProperty: (_, property) => Reflect.deleteProperty(getter(), property),
    ownKeys: (_) => Reflect.ownKeys(getter()),
    construct: (_, argArray, newTarget) => Reflect.construct(getter(), argArray, newTarget),
    apply: (_, thisArg, args) => Reflect.apply(getter(), thisArg, args),
  });
}
module.exports = (getter) => _permaproxy(getter, {});
module.exports.callable = (getter) => _permaproxy(getter, function () {});
