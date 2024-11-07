/* eslint-disable @typescript-eslint/ban-types */
import { debugLogger, optimizeTracing, iterateSorted } from '../utils';
import type { ReadonlyEmitter } from './Emitter';

//#region Optimized event helpers

const __CATEGORY_LISTENERS = ['listeners'];
const __LISTENERS = optimizeTracing((listener: unknown) => ({
  cat: __CATEGORY_LISTENERS,
  fn: `${listener}`,
}));

//#endregion

const ONCE: unique symbol = Symbol('ONCE');

export abstract class ReadonlyEmitterBase<EventMap> implements ReadonlyEmitter<EventMap> {
  protected readonly _log: typeof debugLogger;
  protected readonly _listeners: Map<keyof EventMap | '*', [Function, number][]> = new Map();

  #listenersCounter = 0;

  constructor(name: string) {
    this._log = debugLogger.child({
      cat: `emitter`,
      tid: [name, {}],
    });

    this._listeners.set('*', []);
  }

  on<K extends keyof EventMap>(
    type: K | '*',
    listener: Function & { [ONCE]?: true },
    order?: number,
  ): this {
    if (!listener[ONCE]) {
      this._log.trace(__LISTENERS(listener), 'on(%s)', type);
    }

    if (!this._listeners.has(type)) {
      this._listeners.set(type, []);
    }

    const listeners = this._listeners.get(type)!;
    listeners.push([listener, order ?? this.#listenersCounter++]);
    listeners.sort((a, b) => getOrder(a) - getOrder(b));

    return this;
  }

  once<K extends keyof EventMap>(type: K | '*', listener: Function, order?: number): this {
    this._log.trace(__LISTENERS(listener), 'once(%s)', type);
    return this.on(type, this.#createOnceListener(type, listener), order);
  }

  off<K extends keyof EventMap>(
    type: K | '*',
    listener: Function & { [ONCE]?: true },
    _order?: number,
  ): this {
    if (!listener[ONCE]) {
      this._log.trace(__LISTENERS(listener), 'off(%s)', type);
    }

    const listeners = this._listeners.get(type) || [];
    const index = listeners.findIndex(([l]) => l === listener);
    if (index !== -1) {
      listeners.splice(index, 1);
    }
    return this;
  }

  protected *_getListeners<K extends keyof EventMap>(type: K): Iterable<Function> {
    const wildcard: [Function, number][] = this._listeners.get('*') ?? [];
    const named: [Function, number][] = this._listeners.get(type) ?? [];
    for (const [listener] of iterateSorted<[Function, number]>(getOrder, wildcard, named)) {
      yield listener;
    }
  }

  #createOnceListener<K extends keyof EventMap>(type: K | '*', listener: Function) {
    const onceListener = ((event: Event) => {
      this.off(type, onceListener);
      return listener(event);
    }) as Function & { [ONCE]?: true };
    onceListener.toString = listener.toString.bind(listener);
    onceListener[ONCE] = true as const;
    return onceListener;
  }
}

function getOrder<T>([_a, b]: [T, number]): number {
  return b;
}
