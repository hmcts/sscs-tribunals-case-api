/// <reference types="node" />
import type { Readable } from 'node:stream';
import type { TraceMergeOptions } from './streams';
export declare function uniteTraceEvents(sourcePaths: string[], options?: TraceMergeOptions): Readable;
export declare function uniteTraceEventsToFile(sourcePaths: string[], destinationPath: string, options?: TraceMergeOptions): Promise<unknown>;
