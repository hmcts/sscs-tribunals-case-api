import type { Emitter } from './Emitter';
import { logError } from './logError';
import { ReadonlyEmitterBase } from './ReadonlyEmitterBase';
import { __EMIT, __ENQUEUE, __INVOKE } from './syncEmitterCommons';

/**
 * An event emitter that emits events in the order they are received.
 * If an event is emitted while another event is being emitted, the new event
 * will be queued and emitted after the current event is finished.
 */
export class SerialSyncEmitter<EventMap>
  extends ReadonlyEmitterBase<EventMap>
  implements Emitter<EventMap>
{
  #queue: [keyof EventMap, EventMap[keyof EventMap]][] = [];

  emit<K extends keyof EventMap>(nextEventType: K, nextEvent: EventMap[K]): void {
    this.#queue.push([nextEventType, Object.freeze(nextEvent as any)]);

    if (this.#queue.length > 1) {
      this._log.trace(__ENQUEUE(nextEvent), `enqueue(${String(nextEventType)})`);
      return;
    }

    while (this.#queue.length > 0) {
      const [eventType, event] = this.#queue[0];
      const listeners = [...this._getListeners(eventType)];
      const $eventType = String(eventType);

      this._log.trace.complete(__EMIT(event), $eventType, () => {
        if (listeners) {
          for (const listener of listeners) {
            try {
              this._log.trace.complete(__INVOKE(listener), 'invoke', () => listener(event));
            } catch (error: unknown) {
              logError(error, $eventType, listener);
            }
          }
        }
      });

      this.#queue.shift();
    }
  }
}
