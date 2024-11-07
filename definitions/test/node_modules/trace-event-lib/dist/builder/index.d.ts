import { AsyncEndEvent, AsyncInstantEvent, AsyncStartEvent, CompleteEvent, CounterEvent, DurationBeginEvent, DurationEndEvent, Event, InstantEvent, MetadataEvent } from '../schema';
export type OmitOptionally<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;
export type Completable<T extends Event> = OmitOptionally<T, 'ts' | 'pid' | 'tid'>;
export type Simplified<T extends Event> = OmitOptionally<T, 'ts' | 'pid' | 'tid' | 'ph'>;
export type AutocompletedEventFields = Pick<Event, 'ts' | 'pid' | 'tid'>;
export declare abstract class AbstractEventBuilder {
    begin(event: Simplified<DurationBeginEvent>): void;
    beginAsync(event: Simplified<AsyncStartEvent>): void;
    complete(event: Simplified<CompleteEvent>): void;
    counter(event: Simplified<CounterEvent>): void;
    end(event?: Simplified<DurationEndEvent>): void;
    endAsync(event: Simplified<AsyncEndEvent>): void;
    instant(event: Simplified<InstantEvent>): void;
    instantAsync(event: Simplified<AsyncInstantEvent>): void;
    metadata<T extends MetadataEvent>(event: Simplified<T>): void;
    process_labels(labels: string[], pid?: number): void;
    process_name(name: string, pid?: number): void;
    process_sort_index(index: number, pid?: number): void;
    thread_name(name: string, tid?: number, pid?: number): void;
    thread_sort_index(index: number, tid?: number, pid?: number): void;
    event<T extends Event>(event: Completable<T>): void;
    protected defaults<T extends Partial<Event>>(event: T): T & AutocompletedEventFields;
    protected abstract send<T extends Event>(event: T): void;
}
