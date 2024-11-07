import { assertFunction, assertNumber, assertString } from '../utils';
import type { ReadonlyAsyncEmitter } from './AsyncEmitter';
import type { ReadonlyEmitter } from './Emitter';
import { SerialAsyncEmitter } from './SerialAsyncEmitter';
import { SerialSyncEmitter } from './SerialSyncEmitter';

export type ReadonlySemiAsyncEmitter<AsyncMap, SyncMap> = ReadonlyAsyncEmitter<AsyncMap> &
  ReadonlyEmitter<SyncMap>;

export class SemiAsyncEmitter<AsyncMap, SyncMap>
  implements ReadonlyAsyncEmitter<AsyncMap>, ReadonlyEmitter<SyncMap>
{
  readonly #asyncEmitter: SerialAsyncEmitter<AsyncMap>;
  readonly #syncEmitter: SerialSyncEmitter<SyncMap>;
  readonly #syncEvents: Set<keyof SyncMap>;

  constructor(name: string, syncEvents: Iterable<keyof SyncMap>) {
    this.#asyncEmitter = new SerialAsyncEmitter(name);
    this.#syncEmitter = new SerialSyncEmitter(name);
    this.#syncEvents = new Set(syncEvents);
  }

  on<K extends keyof SyncMap>(
    type: K | '*',
    listener: (event: SyncMap[K]) => unknown,
    order?: number,
  ): this;
  on<K extends keyof AsyncMap>(
    type: K | '*',
    listener: (event: AsyncMap[K]) => unknown,
    order?: number,
  ): this;
  on<K extends keyof (AsyncMap & SyncMap)>(
    type: K | '*',
    listener: (event: any) => unknown,
    order?: number,
  ): this {
    assertString(type, 'type');
    assertFunction(listener, 'listener');
    order !== undefined && assertNumber(order, 'order');

    return this.#invoke('on', type, listener, order);
  }

  once<K extends keyof SyncMap>(
    type: K | '*',
    listener: (event: SyncMap[K]) => unknown,
    order?: number,
  ): this;
  once<K extends keyof AsyncMap>(
    type: K | '*',
    listener: (event: AsyncMap[K]) => unknown,
    order?: number,
  ): this;
  once<K extends keyof (AsyncMap & SyncMap)>(
    type: K | '*',
    listener: (event: any) => unknown,
    order?: number,
  ): this {
    assertString(type, 'type');
    assertFunction(listener, 'listener');
    order !== undefined && assertNumber(order, 'order');

    return this.#invoke('once', type, listener, order);
  }

  off<K extends keyof SyncMap>(type: K | '*', listener: (event: SyncMap[K]) => unknown): this;
  off<K extends keyof AsyncMap>(type: K | '*', listener: (event: AsyncMap[K]) => unknown): this;
  off<K extends keyof (AsyncMap & SyncMap)>(
    type: K | '*',
    listener: (event: any) => unknown,
  ): this {
    assertString(type, 'type');
    assertFunction(listener, 'listener');

    return this.#invoke('off', type, listener);
  }

  emit<K extends keyof SyncMap>(type: K, event: SyncMap[K]): void;
  emit<K extends keyof AsyncMap>(type: K, event: AsyncMap[K]): Promise<void>;
  emit<K extends keyof (AsyncMap & SyncMap)>(type: K, event: any): void | Promise<void> {
    assertString(type, 'type');

    return this.#syncEvents.has(type as keyof SyncMap)
      ? this.#syncEmitter.emit(type as keyof SyncMap, event)
      : this.#asyncEmitter.emit(type as keyof AsyncMap, event);
  }

  #invoke<K extends keyof (AsyncMap & SyncMap)>(
    methodName: 'on' | 'once' | 'off',
    type: K | '*',
    listener: (event: any) => unknown,
    order?: number,
  ): this {
    const isSync = this.#syncEvents.has(type as keyof SyncMap);

    if (type === '*' || isSync) {
      this.#syncEmitter[methodName](type as keyof SyncMap, listener, order);
    }

    if (type === '*' || !isSync) {
      this.#asyncEmitter[methodName](
        type as keyof AsyncMap,
        listener as (event: Event) => Promise<void>,
        order,
      );
    }

    return this;
  }
}
