import type { ThreadAlias } from '../../types';

export type BunyaminLogRecordFields = {
  [customProperty: string]: unknown;

  /**
   * Manual override for Process ID.
   * Not recommended for use.
   */
  pid?: number;

  /**
   * Thread ID.
   * Since JavaScript is normally single-threaded, this concept is rather
   * a pseudo-thread, used to track asynchronous operations in the log.
   * Use {@link ExplicitThreadID} when you want to manually specify the Thread ID.
   * Use {@link ThreadAlias} when you want to automatically allocate the Thread ID.
   * If your pseudo-thread has concurrency, use complex thread aliases to avoid
   * misattribution of begin/end events.
   * @example 123
   * @example 'child_process'
   * @example ['child_process', cpid]
   */
  tid?: number | ThreadAlias;

  /**
   * Event categories (tags) to facilitate filtering.
   */
  cat?: string | string[];

  /**
   * Color name (applicable in Google Chrome Trace Format)
   */
  cname?: string;

  /**
   * @deprecated Cannot manually override Event Phase per Google Chrome Trace Format.
   * Use bunyamin[level].begin(...), bunyamin[level].end(...) or bunyamin[level].complete(...)
   * instead.
   */
  ph?: never;

  /**
   * Manual override for timestamp.
   * The value should be either:
   * 1) in ISO 8601 Extended Format
   * 2) in UTC, as from Date.toISOString().
   * @deprecated Not recommended for use.
   * @example
   * '2020-01-01T00:00:00.000Z'
   */
  time?: string;
};
