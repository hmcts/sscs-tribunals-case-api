import { EventWithStack } from './EventWithStack';

/**
 * The instant events correspond to something that happens but has no duration associated with it.
 * For example, vblank events are considered instant events.
 */
export interface InstantEvent extends EventWithStack {
  /** @inheritDoc */
  ph: 'i';
  /** @inheritDoc */
  name: string;
  /**
   * The scope of the event.
   * There are four scopes available global (g), process (p) and thread (t).
   * If no scope is provided we default to thread scoped events.
   */
  s?: 'g' | 'p' | 't';
}

/** @inheritDoc */
export interface GlobalInstantEvent extends InstantEvent {
  /** @inheritDoc */
  s: 'g';

  /**
   * Global-scoped events do not support stack traces.
   */

  sf: never;
  /**
   * Global-scoped events do not support stack traces.
   */
  stack: never;
}

/** @inheritDoc */
export interface ProcessInstantEvent extends InstantEvent {
  /** @inheritDoc */
  s: 'p';

  /**
   * Process-scoped events do not support stack traces.
   */
  sf: never;

  /**
   * Process-scoped events do not support stack traces.
   */
  stack: never;
}

/** @inheritDoc */
export interface ThreadInstantEvent extends InstantEvent {
  /** @inheritDoc */
  s?: 't';
}
