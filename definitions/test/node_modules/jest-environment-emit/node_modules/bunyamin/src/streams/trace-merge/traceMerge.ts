import type { Readable } from 'node:stream';

import { jsonlReadFile } from '../jsonl';
import type { Resolver } from './resolvers';
import { FilePIDResolver, PIDResolver } from './resolvers';
import { multisort, TraceAnalyze, TraceMerge } from './transforms';

export type TraceMergeOptions = {
  mode: 'pid' | 'fpid';
};

export function traceMerge(filePaths: string[], options?: TraceMergeOptions): Readable {
  const streams = filePaths.map((filePath) => jsonlReadFile(filePath));
  const resolver = makeResolver(options);
  const $resolver = makeDeferred<Resolver>();
  const analyze = new TraceAnalyze(resolver)
    .on('error', (error) => $resolver.reject(error))
    .on('finish', () => $resolver.resolve(resolver));

  const merge = new TraceMerge($resolver.promise);

  const sorted = multisort(streams);
  sorted.pipe(analyze);
  return sorted.pipe(merge);
}

function makeResolver(options?: TraceMergeOptions): Resolver {
  return options?.mode === 'fpid' ? new FilePIDResolver() : new PIDResolver();
}

function makeDeferred<T>() {
  let resolve: (value: T) => void;
  let reject: (reason?: unknown) => void;
  const promise = new Promise<T>((_resolve, _reject) => {
    resolve = _resolve;
    reject = _reject;
  });

  return {
    promise: promise,
    resolve: resolve!,
    reject: reject!,
  };
}
