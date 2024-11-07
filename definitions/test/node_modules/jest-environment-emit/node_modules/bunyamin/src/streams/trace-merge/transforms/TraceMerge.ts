import { Transform } from 'node:stream';
import type { TraceEvent } from 'trace-event-lib';

import type { Resolver } from '../resolvers';
import type { JSONLEntry } from '../../jsonl';

export class TraceMerge extends Transform {
  readonly #resolverPromise: Promise<Resolver>;
  #resolver?: Resolver;

  constructor(resolverPromise: Promise<Resolver>) {
    super({
      objectMode: true,
      highWaterMark: Number.MAX_SAFE_INTEGER,
    });

    this.#resolverPromise = resolverPromise;
  }

  _transform(
    chunk: unknown,
    _encoding: string,
    callback: (error?: Error | null, data?: unknown) => void,
  ) {
    if (this.#resolver) {
      const entry = chunk as JSONLEntry<TraceEvent>;
      const output = { ...entry.value };
      if (output.pid != null) {
        output.pid = this.#resolver.resolvePid(entry.filePath, entry.value.pid);
      }
      if (output.tid != null) {
        output.tid = this.#resolver.resolveTid(entry.filePath, entry.value.pid, entry.value.tid);
      }
      this.push(output);
      callback();
    } else {
      this.#resolverPromise.then(
        (resolver) => {
          this.#resolver = resolver;
          this._transform(chunk, _encoding, callback);
        },
        (error) => {
          callback(error);
        },
      );
    }
  }
}
