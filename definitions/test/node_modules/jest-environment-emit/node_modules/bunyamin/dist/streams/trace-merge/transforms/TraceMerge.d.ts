/// <reference types="node" />
import { Transform } from 'node:stream';
import type { Resolver } from '../resolvers';
export declare class TraceMerge extends Transform {
    #private;
    constructor(resolverPromise: Promise<Resolver>);
    _transform(chunk: unknown, _encoding: string, callback: (error?: Error | null, data?: unknown) => void): void;
}
