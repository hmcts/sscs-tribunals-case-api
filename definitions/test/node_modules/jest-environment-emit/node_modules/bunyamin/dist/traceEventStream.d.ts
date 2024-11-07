/// <reference types="node" />
import type { Transform } from 'node:stream';
import type { TraceEventStreamOptions } from './streams';
export declare function traceEventStream(options: TraceEventStreamOptions & {
    filePath: string;
}): Transform;
