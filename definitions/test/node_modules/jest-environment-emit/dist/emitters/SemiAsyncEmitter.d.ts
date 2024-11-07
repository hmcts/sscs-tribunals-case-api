import type { ReadonlyAsyncEmitter } from './AsyncEmitter';
import type { ReadonlyEmitter } from './Emitter';
export type ReadonlySemiAsyncEmitter<AsyncMap, SyncMap> = ReadonlyAsyncEmitter<AsyncMap> & ReadonlyEmitter<SyncMap>;
export declare class SemiAsyncEmitter<AsyncMap, SyncMap> implements ReadonlyAsyncEmitter<AsyncMap>, ReadonlyEmitter<SyncMap> {
    #private;
    constructor(name: string, syncEvents: Iterable<keyof SyncMap>);
    on<K extends keyof SyncMap>(type: K | '*', listener: (event: SyncMap[K]) => unknown, order?: number): this;
    on<K extends keyof AsyncMap>(type: K | '*', listener: (event: AsyncMap[K]) => unknown, order?: number): this;
    once<K extends keyof SyncMap>(type: K | '*', listener: (event: SyncMap[K]) => unknown, order?: number): this;
    once<K extends keyof AsyncMap>(type: K | '*', listener: (event: AsyncMap[K]) => unknown, order?: number): this;
    off<K extends keyof SyncMap>(type: K | '*', listener: (event: SyncMap[K]) => unknown): this;
    off<K extends keyof AsyncMap>(type: K | '*', listener: (event: AsyncMap[K]) => unknown): this;
    emit<K extends keyof SyncMap>(type: K, event: SyncMap[K]): void;
    emit<K extends keyof AsyncMap>(type: K, event: AsyncMap[K]): Promise<void>;
}
