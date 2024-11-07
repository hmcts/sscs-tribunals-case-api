import type { Event } from 'trace-event-lib';
import * as TEL from 'trace-event-lib';
import type { Transform } from 'node:stream';

export class StreamEventBuilder extends TEL.AbstractEventBuilder {
  constructor(protected readonly stream: Transform) {
    super();
  }

  public send(event: Event) {
    this.stream.push(event);
  }
}
