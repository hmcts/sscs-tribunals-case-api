/* eslint-disable @typescript-eslint/ban-types */
export function getHierarchy(instance: any): Function[] {
  const hierarchy: Function[] = [];
  let currentClass = instance?.constructor;

  while (typeof currentClass === 'function') {
    hierarchy.push(currentClass);
    currentClass = Object.getPrototypeOf(currentClass);
  }

  return hierarchy.reverse();
}
