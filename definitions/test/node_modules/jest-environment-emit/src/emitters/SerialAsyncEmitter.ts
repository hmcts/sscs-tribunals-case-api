import type { AsyncEmitter } from './AsyncEmitter';
import { logError } from './logError';
import { ReadonlyEmitterBase } from './ReadonlyEmitterBase';
import { __EMIT, __INVOKE } from './syncEmitterCommons';

export class SerialAsyncEmitter<EventMap>
  extends ReadonlyEmitterBase<EventMap>
  implements AsyncEmitter<EventMap>
{
  #idle?: Promise<void>;
  readonly #tasks: Promise<void>[] = [];

  emit<K extends keyof EventMap>(eventType: K, event: EventMap[K]): Promise<void> {
    this.#tasks.push(this.#doEmit(eventType, event));
    this.#idle ??= this.#waitForIdle();
    return this.#idle;
  }

  async #waitForIdle() {
    do {
      const $promises = new Set(this.#tasks);
      await Promise.all(this.#tasks);
      for (let index = this.#tasks.length - 1; index >= 0; index--) {
        if ($promises.has(this.#tasks[index])) {
          this.#tasks.splice(index, 1);
        }
      }
    } while (this.#tasks.length > 0);
    this.#idle = undefined;
  }

  async #doEmit<K extends keyof EventMap>(eventType: K, event: EventMap[K]) {
    const listeners = [...this._getListeners(eventType)];
    const $eventType = String(eventType);

    await this._log.trace.complete(__EMIT(event), $eventType, async () => {
      if (listeners) {
        for (const listener of listeners) {
          try {
            await this._log.trace.complete(__INVOKE(listener), 'invoke', () => listener(event));
          } catch (error: unknown) {
            logError(error, $eventType, listener);
          }
        }
      }
    });
  }
}
