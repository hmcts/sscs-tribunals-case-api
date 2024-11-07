/// <reference types="node" />
/// <reference types="node" />
/// <reference types="node" />
import { Serializer } from 'bunyan';
import { Writable } from 'stream';
import type { WriteStream } from 'tty';
interface Stringifier {
    (obj: any, options: {
        entry: any;
        useColor: boolean;
        debugStream: BunyanDebugStream;
    }): string | {
        consumed?: string[];
        value: string;
        replaceMessage?: boolean;
    } | null | undefined;
}
export interface BunyanDebugStreamOptions {
    colors?: {
        [key: number]: string | string[];
    } | false | null;
    forceColor?: boolean;
    basepath?: string;
    basepathReplacement?: string;
    showProcess?: boolean;
    showDate?: boolean | ((time: Date, entry: any) => string);
    showPrefixes?: boolean | ((prefixes: string[]) => string);
    processName?: string;
    maxExceptionLines?: number | 'auto';
    stringifiers?: {
        [key: string]: Stringifier | null;
    };
    prefixers?: {
        [key: string]: Stringifier | null;
    };
    out?: WriteStream;
    indent?: string;
    showLoggerName?: boolean;
    showPid?: boolean;
    showLevel?: boolean;
    showMetadata?: boolean;
}
declare class BunyanDebugStream extends Writable {
    options: BunyanDebugStreamOptions;
    private _colors;
    private _useColor;
    private _stringifiers;
    private _prefixers;
    private _processName;
    private _out;
    private _basepath;
    private _indent;
    private _showDate;
    private _showPrefixes;
    private _showLoggerName;
    private _showPid;
    private _showLevel;
    private _showMetadata;
    constructor(options?: BunyanDebugStreamOptions);
    private _runStringifier;
    private _entryToString;
    _write(entry: any, _encoding: string, done: () => void): void;
}
export declare const serializers: {
    [key: string]: Serializer;
};
export declare const stdStringifiers: {
    [key: string]: Stringifier;
};
export declare function create(options: BunyanDebugStreamOptions): NodeJS.WritableStream;
export default create;
