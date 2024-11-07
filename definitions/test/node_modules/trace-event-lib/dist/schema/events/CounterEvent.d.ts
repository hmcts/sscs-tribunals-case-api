import { Event } from './Event';
/**
 * The counter events can track a value or multiple values as they change over time.
 * Counter events are specified with the C phase type.
 * Each counter can be provided with multiple series of data to display.
 * When multiple series are provided they will be displayed as a stacked area chart in Trace Viewer.
 * Please note that counters are process-local.
 */
export interface CounterEvent extends Event {
    /** @inheritDoc */
    ph: 'C';
    /** @inheritDoc */
    name: string;
    /**
     * When an id field exists, the combination of the event name and id is used as the counter name.
     */
    id?: number;
    /**
     * Reportedly, counter events do not have categories.
     */
    cat?: never;
}
