/// <reference types="node" />
import { Transform } from 'node:stream';
import type { TraceEventStreamOptions } from './options/TraceEventStreamOptions';
export declare class BunyanTraceEventStream extends Transform {
    #private;
    constructor(userOptions?: TraceEventStreamOptions);
    _transform(record: unknown, _encoding: string, callback: (error?: Error | null, data?: unknown) => void): void;
}
