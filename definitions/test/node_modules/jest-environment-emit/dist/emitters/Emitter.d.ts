export interface ReadonlyEmitter<EventMap> {
    on<K extends keyof EventMap>(type: K | '*', listener: (event: EventMap[K]) => unknown, order?: number): this;
    once<K extends keyof EventMap>(type: K | '*', listener: (event: EventMap[K]) => unknown, order?: number): this;
    off<K extends keyof EventMap>(type: K | '*', listener: (event: EventMap[K]) => unknown): this;
}
export interface Emitter<EventMap> extends ReadonlyEmitter<EventMap> {
    emit<K extends keyof EventMap>(type: K, event: EventMap[K]): void;
}
