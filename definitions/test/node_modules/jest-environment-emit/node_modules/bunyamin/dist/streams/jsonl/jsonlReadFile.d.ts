/// <reference types="node" />
import type { Readable } from 'node:stream';
export declare function jsonlReadFile(filePath: string): Readable;
export type JSONLEntry<T = unknown> = {
    filePath: string;
    key: number;
    value: T;
};
