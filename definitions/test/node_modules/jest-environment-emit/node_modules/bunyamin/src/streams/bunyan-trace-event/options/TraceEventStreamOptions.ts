import type { ThreadGroupConfig } from '../threads';

export type TraceEventStreamOptions = {
  /**
   * @default ['v', 'hostname', 'level', 'name']
   */
  ignoreFields?: string[];
  /**
   * Thread groups allow you to use non-numeric thread IDs (aliases) in your
   * logs. This is useful when you have multiple asynchronous operations
   * running in parallel, and you want to group them together in the trace
   * viewer under the same thread name and keep the thread IDs together.
   */
  threadGroups?: Iterable<string | ThreadGroupConfig>;
  /**
   * Default maximum number of concurrent threads in each thread group.
   * Must be a positive integer.
   * @default 100
   */
  maxConcurrency?: number;
  /**
   * Default thread name.
   * @default 'Main Thread'
   */
  defaultThreadName?: string;
  /**
   * Strict mode. If enabled, throws an error when a thread group ID (alias)
   * is out of available thread IDs (see `maxConcurrency`). Otherwise, the
   * thread ID is resolved to the maximum available thread ID.
   */
  strict?: boolean;
};
