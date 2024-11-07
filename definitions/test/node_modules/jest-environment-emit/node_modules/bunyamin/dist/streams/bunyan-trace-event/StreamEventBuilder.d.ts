/// <reference types="node" />
import type { Event } from 'trace-event-lib';
import * as TEL from 'trace-event-lib';
import type { Transform } from 'node:stream';
export declare class StreamEventBuilder extends TEL.AbstractEventBuilder {
    protected readonly stream: Transform;
    constructor(stream: Transform);
    send(event: Event): void;
}
