/// <reference types="node" />
import { Writable } from 'node:stream';
import type { Resolver } from '../resolvers';
export declare class TraceAnalyze extends Writable {
    #private;
    constructor(resolver: Resolver);
    _write(chunk: unknown, _encoding: string, callback: (error?: Error | null, data?: unknown) => void): void;
    _final(callback: (error?: Error | null) => void): void;
}
