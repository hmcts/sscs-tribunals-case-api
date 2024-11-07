import { Transform } from 'node:stream';

import { isError } from '../../utils';

import { ThreadGroupDispatcher } from './threads';
import type { ThreadGroupConfig } from './threads';
import { bunyan2trace } from './bunyan2trace';
import { StreamEventBuilder } from './StreamEventBuilder';
import type { TraceEventStreamOptions } from './options/TraceEventStreamOptions';
import { normalizeOptions } from './options/normalizeOptions';

// TODO: add tests
export class BunyanTraceEventStream extends Transform {
  readonly #knownTids = new Set<number>();
  readonly #threadGroupDispatcher: ThreadGroupDispatcher;
  readonly #builder = new StreamEventBuilder(this);
  readonly #ignoreFields: string[];

  #started = false;

  constructor(userOptions: TraceEventStreamOptions = {}) {
    super({ objectMode: true });

    const options = normalizeOptions(userOptions);
    this.#ignoreFields = options.ignoreFields;
    this.#threadGroupDispatcher = new ThreadGroupDispatcher({
      strict: options.strict ?? false,
      defaultThreadName: options.defaultThreadName ?? 'Main Thread',
      maxConcurrency: options.maxConcurrency ?? 100,
      // Lazy to add a `NormalizedOptions...` type, so we just cast it here.
      threadGroups: options.threadGroups as Iterable<ThreadGroupConfig>,
    });
  }

  _transform(
    record: unknown,
    _encoding: string,
    callback: (error?: Error | null, data?: unknown) => void,
  ) {
    const json = typeof record === 'string' ? JSON.parse(record) : record;
    const event = json && bunyan2trace(json);

    if (event.args) {
      for (const field of this.#ignoreFields) {
        delete event.args[field];
      }
    }

    if (!this.#started) {
      this.#started = true;
      this.#builder.metadata({
        pid: event.pid,
        ts: event.ts,
        name: 'process_name',
        args: { name: json.name },
      });
    }

    const tid = (event.tid = this.#threadGroupDispatcher.resolve(event.ph, event.tid));
    if (isError(tid)) {
      callback(tid);
      return;
    }

    if (!this.#knownTids.has(tid)) {
      this.#knownTids.add(tid);

      const threadName = this.#threadGroupDispatcher.name(tid);
      if (threadName) {
        this.#builder.metadata({
          pid: event.pid,
          tid: event.tid,
          ts: event.ts,
          name: 'thread_name',
          args: { name: threadName },
        });
      }
    }

    this.#builder.send(event);
    callback(null);
  }
}
