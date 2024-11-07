import {
  AsyncEndEvent,
  AsyncInstantEvent,
  AsyncStartEvent,
  CompleteEvent,
  CounterEvent,
  DurationBeginEvent,
  DurationEndEvent,
  Event,
  InstantEvent,
  MetadataEvent,
  MetadataProcessLabelsEvent,
  MetadataProcessNameEvent,
  MetadataProcessSortIndexEvent,
  MetadataThreadNameEvent,
  MetadataThreadSortIndexEvent,
} from '../schema';

import { compactObject, getProcessId, now } from '../utils';

export type OmitOptionally<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;

export type Completable<T extends Event> = OmitOptionally<T, 'ts' | 'pid' | 'tid'>;

export type Simplified<T extends Event> = OmitOptionally<T, 'ts' | 'pid' | 'tid' | 'ph'>;

export type AutocompletedEventFields = Pick<Event, 'ts' | 'pid' | 'tid'>;

export abstract class AbstractEventBuilder {
  public begin(event: Simplified<DurationBeginEvent>): void {
    const { args, tts, ts, cat, name, sf, cname, pid, tid, stack } = this.defaults(event);

    this.event({
      ph: 'B',
      args,
      tts,
      ts,
      cat,
      name,
      sf,
      cname,
      pid,
      tid,
      stack,
    } as DurationBeginEvent);
  }

  public beginAsync(event: Simplified<AsyncStartEvent>): void {
    const { args, cat, name, pid, tid, ts, tts, cname, id, id2, scope } = this.defaults(event);

    this.event({
      ph: 'b',
      args,
      cat,
      name,
      pid,
      tid,
      ts,
      tts,
      cname,
      id,
      id2,
      scope,
    } as AsyncStartEvent);
  }

  public complete(event: Simplified<CompleteEvent>): void {
    const { tid, pid, ts, args, tts, sf, cname, dur, stack, esf, cat, name, tdur, estack } =
      this.defaults(event);

    this.event({
      ph: 'X',
      args,
      cat,
      cname,
      dur,
      esf,
      estack,
      name,
      pid,
      sf,
      stack,
      tdur,
      tid,
      ts,
      tts,
    } as CompleteEvent);
  }

  public counter(event: Simplified<CounterEvent>): void {
    const { args, cat, cname, pid, tid, ts, tts, name, id } = this.defaults(event);
    this.event({ ph: 'C', args, cat, cname, pid, tid, ts, tts, name, id } as CounterEvent);
  }

  public end(event: Simplified<DurationEndEvent> = {}): void {
    const { args, tts, ts, sf, cname, pid, tid, stack } = this.defaults(event);
    this.event({ ph: 'E', args, tts, ts, sf, cname, pid, tid, stack } as DurationEndEvent);
  }

  public endAsync(event: Simplified<AsyncEndEvent>): void {
    const { args, tts, tid, ts, pid, cname, id, id2, scope, cat, name } = this.defaults(event);
    this.event({
      ph: 'e',
      args,
      tts,
      tid,
      ts,
      pid,
      cname,
      id,
      id2,
      scope,
      cat,
      name,
    } as AsyncEndEvent);
  }

  public instant(event: Simplified<InstantEvent>): void {
    const { args, sf, cname, pid, tid, ts, tts, cat, name, s, stack } = this.defaults(event);

    this.event({
      ph: 'i',
      args,
      cat,
      cname,
      name,
      pid,
      s,
      sf,
      stack,
      tid,
      ts,
      tts,
    } as InstantEvent);
  }

  public instantAsync(event: Simplified<AsyncInstantEvent>): void {
    const { args, cat, name, pid, tid, ts, tts, cname, id, id2, scope } = this.defaults(event);
    this.event({
      ph: 'n',
      args,
      cat,
      name,
      pid,
      tid,
      ts,
      tts,
      cname,
      id,
      id2,
      scope,
    } as AsyncInstantEvent);
  }

  public metadata<T extends MetadataEvent>(event: Simplified<T>): void {
    const { args, tts, ts, tid, pid, cname, cat, name } = this.defaults(event);
    this.event({ ph: 'M', args, tts, ts, tid, pid, cname, cat, name } as MetadataEvent);
  }

  public process_labels(labels: string[], pid?: number): void {
    this.metadata<MetadataProcessLabelsEvent>({
      pid,
      name: 'process_labels',
      args: { labels: labels.join(',') },
    });
  }

  public process_name(name: string, pid?: number): void {
    this.metadata<MetadataProcessNameEvent>({
      pid,
      name: 'process_name',
      args: { name },
    });
  }

  public process_sort_index(index: number, pid?: number): void {
    this.metadata<MetadataProcessSortIndexEvent>({
      pid,
      name: 'process_sort_index',
      args: { sort_index: index },
    });
  }

  public thread_name(name: string, tid?: number, pid?: number): void {
    this.metadata<MetadataThreadNameEvent>({
      pid,
      tid,
      name: 'thread_name',
      args: { name },
    });
  }

  public thread_sort_index(index: number, tid?: number, pid?: number): void {
    this.metadata<MetadataThreadSortIndexEvent>({
      pid,
      tid,
      name: 'thread_sort_index',
      args: { sort_index: index },
    });
  }

  public event<T extends Event>(event: Completable<T>): void {
    this.send(compactObject(this.defaults(event) as T));
  }

  protected defaults<T extends Partial<Event>>(event: T): T & AutocompletedEventFields {
    const { ts = now(), pid = getProcessId(), tid = 0 } = event;

    return {
      ...event,

      ts,
      pid,
      tid,
    } as any; // eslint-disable-line @typescript-eslint/no-explicit-any
  }

  protected abstract send<T extends Event>(event: T): void;
}
