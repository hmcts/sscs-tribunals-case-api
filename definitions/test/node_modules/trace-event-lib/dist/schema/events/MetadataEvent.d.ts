import { Event } from './Event';
/**
 * Metadata events are used to associate extra information with the events in the trace file.
 * This information can be things like process names, or thread names.
 */
export interface MetadataEvent extends Event {
    /** @inheritDoc */
    ph: 'M';
    cat?: never;
}
/**
 * Sets the display name for the provided {@link Event#pid}..
 */
export interface MetadataProcessNameEvent extends MetadataEvent {
    name: 'process_name';
    args: {
        name: string;
    };
}
/**
 * Sets the extra process labels for the provided {@link Event#pid}.
 */
export interface MetadataProcessLabelsEvent extends MetadataEvent {
    name: 'process_labels';
    args: {
        /**
         * TODO: probably, comma-separated values string?
         */
        labels: unknown;
    };
}
/**
 * Sets the process sort order position.
 */
export interface MetadataProcessSortIndexEvent extends MetadataEvent {
    name: 'process_sort_index';
    args: {
        /**
         * Lower numbers are displayed higher in Trace Viewer.
         * If multiple items all have the same sort index then they are displayed sorted by name and, given duplicate names, by id.
         */
        sort_index: number;
    };
}
/**
 * Sets the name for the given tid.
 */
export interface MetadataThreadNameEvent extends MetadataEvent {
    name: 'thread_name';
    args: {
        name: string;
    };
}
/**
 * Sets the thread sort order position.
 */
export interface MetadataThreadSortIndexEvent extends MetadataEvent {
    name: 'thread_sort_index';
    args: {
        /**
         * Lower numbers are displayed higher in Trace Viewer.
         * If multiple items all have the same sort index then they are displayed sorted by name and, given duplicate names, by id.
         */
        sort_index: number;
    };
}
