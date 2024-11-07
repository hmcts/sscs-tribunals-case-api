import { Event } from './Event';
export type AsyncID2 = {
    local: string;
} | {
    global: string;
};
/**
 * Async events are used to specify asynchronous operations. e.g. frames in a game, or network I/O.
 * Async events are specified with the b, n and e event types.
 */
export interface AsyncEvent extends Event {
    /** @inheritDoc */
    ph: 'b' | 'n' | 'e';
    /**
     * An additional required parameter id.
     * We consider the events with the same {@link Event#cat} and {@link #id} as events from the same event tree.
     * A nested async event should have the same category and id as its parent.
     * @see {#id2}
     */
    id?: number;
    /**
     * An alternative required parameter id2.
     * Can be used instead of the default {@link #id} field and explicitly specify if it is process-local or global.
     */
    id2?: AsyncID2;
    /**
     * An optional scope string can be specified to avoid {@link #id} conflicts,
     * in which case we consider events with the same {@Link Event#cat}, {@link #scope}, and {@link #id}
     * as events from the same event tree.
     */
    scope?: string;
}
export interface AsyncStartEvent extends AsyncEvent {
    /** @inheritDoc */
    ph: 'b';
}
export interface AsyncInstantEvent extends AsyncEvent {
    /** @inheritDoc */
    ph: 'n';
}
export interface AsyncEndEvent extends AsyncEvent {
    /** @inheritDoc */
    ph: 'e';
}
