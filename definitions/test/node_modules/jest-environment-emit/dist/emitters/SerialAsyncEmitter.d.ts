import type { AsyncEmitter } from './AsyncEmitter';
import { ReadonlyEmitterBase } from './ReadonlyEmitterBase';
export declare class SerialAsyncEmitter<EventMap> extends ReadonlyEmitterBase<EventMap> implements AsyncEmitter<EventMap> {
    #private;
    emit<K extends keyof EventMap>(eventType: K, event: EventMap[K]): Promise<void>;
}
