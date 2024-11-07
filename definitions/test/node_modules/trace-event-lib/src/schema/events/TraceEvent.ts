import { DurationBeginEvent, DurationEndEvent } from './DurationEvent';
import { CompleteEvent } from './CompleteEvent';
import { GlobalInstantEvent, ProcessInstantEvent, ThreadInstantEvent } from './InstantEvent';
import { AsyncStartEvent, AsyncEndEvent, AsyncInstantEvent } from './AsyncEvent';
import { CounterEvent } from './CounterEvent';
import {
  MetadataProcessLabelsEvent,
  MetadataProcessNameEvent,
  MetadataProcessSortIndexEvent,
  MetadataThreadNameEvent,
  MetadataThreadSortIndexEvent,
} from './MetadataEvent';

export type TraceEvent =
  | AsyncEndEvent
  | AsyncInstantEvent
  | AsyncStartEvent
  | CompleteEvent
  | CounterEvent
  | DurationBeginEvent
  | DurationEndEvent
  | GlobalInstantEvent
  | MetadataProcessLabelsEvent
  | MetadataProcessNameEvent
  | MetadataProcessSortIndexEvent
  | MetadataThreadNameEvent
  | MetadataThreadSortIndexEvent
  | ProcessInstantEvent
  | ThreadInstantEvent;
