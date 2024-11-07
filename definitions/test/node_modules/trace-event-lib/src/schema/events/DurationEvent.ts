import { EventWithStack } from './EventWithStack';

/**
 * Duration events provide a way to mark a duration of work on a given thread.
 * The duration events are specified by the B and E phase types.
 *
 * You can nest the B and E events.
 * The B event must come before the corresponding E event.
 * This allows you to capture function calling behaviour on a thread.
 *
 * The timestamps for the duration events must be in increasing order for a given thread.
 * Timestamps in different threads do not have to be in increasing order, just the timestamps within a given thread.
 *
 * If you provide args to both the B and E events then the arguments will be merged.
 * If there is a duplicate argument value provided, the E event argument will be taken
 * and the B event argument will be discarded.
 */
export interface DurationEvent extends EventWithStack {
  /** @inheritDoc */
  ph: 'B' | 'E';
}

export interface DurationBeginEvent extends DurationEvent {
  /** @inheritDoc */
  ph: 'B';
  /** @inheritDoc */
  name: string;
}

export interface DurationEndEvent extends DurationEvent {
  /** @inheritDoc */
  ph: 'E';
  name?: never;
  cat?: never;
}
