import { debugLogger } from '../utils';
import type { ReadonlyEmitter } from './Emitter';
declare const ONCE: unique symbol;
export declare abstract class ReadonlyEmitterBase<EventMap> implements ReadonlyEmitter<EventMap> {
    #private;
    protected readonly _log: typeof debugLogger;
    protected readonly _listeners: Map<keyof EventMap | '*', [Function, number][]>;
    constructor(name: string);
    on<K extends keyof EventMap>(type: K | '*', listener: Function & {
        [ONCE]?: true;
    }, order?: number): this;
    once<K extends keyof EventMap>(type: K | '*', listener: Function, order?: number): this;
    off<K extends keyof EventMap>(type: K | '*', listener: Function & {
        [ONCE]?: true;
    }, _order?: number): this;
    protected _getListeners<K extends keyof EventMap>(type: K): Iterable<Function>;
}
export {};
