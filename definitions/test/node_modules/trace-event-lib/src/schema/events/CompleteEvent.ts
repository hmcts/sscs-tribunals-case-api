import { EventWithStack } from './EventWithStack';

/**
 * Each complete event logically combines a pair of {@link DurationBeginEvent} and {@link DurationEndEvent}.
 * In a trace that most of the events are duration events, using complete events to replace the duration events
 * can reduce the size of the trace to about half.
 */
export interface CompleteEvent extends EventWithStack {
  /** @inheritDoc */
  ph: 'X';
  /** @inheritDoc */
  name: string;
  /** @inheritDoc */
  cat: string;

  /**
   * Time of the start of the complete event.
   * Unlike {@link DurationEvent}, the timestamps of complete events can be in any order.
   * @see {Event#ts}
   */
  ts: number;
  /**
   * Specifies the tracing clock duration of complete events in microseconds.
   */
  dur: number;
  /**
   * Specifies the thread clock duration of complete events in microseconds.
   */
  tdur?: number;

  /**
   * Similar to {@link EventWithStack#sf}, but it specifies the end stack trace of the event instead.
   * Mutually exclusive with {@link #estack}.
   */
  esf?: number;
  /**
   * Similar to {@link EventWithStack#stack}, but it specifies the end stack trace of the event instead.
   * Mutually exclusive with {@link #esf}.
   */
  estack?: string[];
}
