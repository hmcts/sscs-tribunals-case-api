export function createIsDebug(namespaces: string) {
  const skips: RegExp[] = [];
  const names: RegExp[] = [];

  for (const part of namespaces.split(/[\s,]+/)) {
    if (!part) {
      continue;
    }

    const destination = part[0] === '-' ? skips : names;
    const pattern = part.replace(/^-/, '').replace(/\*/g, '.*?');
    destination.push(new RegExp(`^${pattern}$`));
  }

  return function isDebug(name: string): boolean {
    // eslint-disable-next-line unicorn/prefer-at
    if (name[name.length - 1] === '*') {
      return true;
    }

    if (skips.some((regex) => regex.test(name))) {
      return false;
    }

    if (names.some((regex) => regex.test(name))) {
      return true;
    }

    return false;
  };
}
