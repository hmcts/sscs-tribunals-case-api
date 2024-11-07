import type { Emitter } from './Emitter';
import { ReadonlyEmitterBase } from './ReadonlyEmitterBase';
export declare class SerialSyncEmitter<EventMap> extends ReadonlyEmitterBase<EventMap> implements Emitter<EventMap> {
    #private;
    emit<K extends keyof EventMap>(nextEventType: K, nextEvent: EventMap[K]): void;
}
