/* eslint-disable @typescript-eslint/no-explicit-any */
const empty = {};
export const noop: (...args: any[]) => any = () => {
  return empty;
};
