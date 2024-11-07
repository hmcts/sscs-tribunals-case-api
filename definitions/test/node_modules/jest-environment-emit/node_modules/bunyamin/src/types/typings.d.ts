declare module 'multi-sort-stream' {
  import type { Readable } from 'node:stream';

  export default function multiSortStream(
    streams: Readable[],
    comparator: (a: unknown, b: unknown) => number,
  ): Readable;
}
