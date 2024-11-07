/// <reference types="node" />
import type { Readable } from 'node:stream';
export type TraceMergeOptions = {
    mode: 'pid' | 'fpid';
};
export declare function traceMerge(filePaths: string[], options?: TraceMergeOptions): Readable;
