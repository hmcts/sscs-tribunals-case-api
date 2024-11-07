import type { Readable } from 'node:stream';
import type { TraceEvent } from 'trace-event-lib';
import multiSortStream from 'multi-sort-stream';

import type { JSONLEntry } from '../../jsonl';

export function multisort(streams: Readable[]): Readable {
  return multiSortStream(streams, comparator);
}

function comparator(a: unknown, b: unknown): number {
  const { value: aa } = a as JSONLEntry<TraceEvent>;
  const { value: bb } = b as JSONLEntry<TraceEvent>;

  return aa.ts < bb.ts ? -1 : aa.ts > bb.ts ? 1 : 0;
}
