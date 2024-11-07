/* eslint-disable unicorn/switch-case-braces,unicorn/prevent-abbreviations,@typescript-eslint/no-explicit-any*/
import type {
  AsyncEvent,
  CompleteEvent,
  CounterEvent,
  DurationBeginEvent,
  DurationEndEvent,
  Event,
  EventWithStack,
  InstantEvent,
  MetadataEvent,
} from 'trace-event-lib';

// TODO: optimize args - they will be often empty objects

export function bunyan2trace(record: any): Event {
  if (!record.ph) {
    return buildFallbackEvent(record);
  }

  switch (record.ph) {
    case 'B':
      return buildDurationBeginEvent(record);
    case 'E':
      return buildDurationEndEvent(record);
    case 'i':
      return buildInstantEvent(record);
    case 'b':
    case 'e':
    case 'n':
      return buildAsyncEvent(record);
    case 'X':
      return buildCompleteEvent(record);
    case 'C':
      return buildCounterEvent(record);
    case 'M':
      return buildMetadataEvent(record);
    default:
      return buildFallbackEvent(record);
  }
}

function buildAsyncEvent(record: any): AsyncEvent {
  const event = bunyan2trace(record) as AsyncEvent;
  return moveProperties(event.args!, event, ['id', 'id2', 'scope']);
}

function buildCompleteEvent(record: any): CompleteEvent {
  const event = extractEventWithStack(record) as CompleteEvent;
  return moveProperties(event.args!, event, ['dur', 'tdur', 'esf', 'estack']);
}

function buildCounterEvent(record: any): CounterEvent {
  const event = bunyan2trace(record) as CounterEvent;
  delete event.cat;
  return moveProperties(event.args!, event, ['id']);
}

function buildDurationBeginEvent(record: any): DurationBeginEvent {
  return extractEventWithStack(record) as DurationBeginEvent;
}

function buildDurationEndEvent(record: any): DurationEndEvent {
  const event = extractEventWithStack(record) as DurationEndEvent;
  delete event.name;
  delete event.cat;
  return event;
}

function buildMetadataEvent(record: any): MetadataEvent {
  const event = bunyan2trace(record) as MetadataEvent;
  delete event.cat;
  return event;
}

function buildInstantEvent(record: any): InstantEvent {
  const event = extractEventWithStack(record) as InstantEvent;
  const args = moveProperties(event.args!, event, ['s']);
  if (args.s === 'g' || args.s === 'p') {
    delete event.sf;
    delete event.stack;
  }

  return event;
}

function buildFallbackEvent(record: any): InstantEvent {
  const event = buildInstantEvent(record);
  event.ph = 'i';
  return event;
}

function extractTraceEvent(record: any): Event {
  const {
    cat,
    cname,
    ph,
    tts,
    pid,
    tid,
    time,
    msg: name,
    name: _processName,
    hostname: _hostname,
    ...args
  } = record;

  const ts = new Date(time).getTime() * 1e3;

  return {
    cat,
    cname,
    ph,
    ts,
    tts,
    pid,
    tid,
    name,
    args,
  } as Event;
}

function extractEventWithStack(record: any): EventWithStack {
  const event = extractTraceEvent(record) as EventWithStack;
  return moveProperties(event.args!, event, ['sf', 'stack']);
}

function moveProperties<T extends Record<string, any>>(
  source: Record<string, any>,
  target: T,
  keys: (keyof T)[],
): T {
  for (const key of keys) {
    if (source[key as string] !== undefined) {
      target[key] = source[key as string];
      delete source[key as string];
    }
  }

  return target;
}
