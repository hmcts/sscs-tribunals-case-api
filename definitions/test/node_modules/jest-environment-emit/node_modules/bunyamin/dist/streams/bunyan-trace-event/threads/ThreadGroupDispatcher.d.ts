import type { ThreadID } from '../../../types';
import type { ThreadGroupConfig } from './ThreadGroupConfig';
export type ThreadGroupDispatcherConfig = {
    defaultThreadName: string;
    maxConcurrency: number;
    strict: boolean;
    threadGroups: Iterable<ThreadGroupConfig>;
};
export declare class ThreadGroupDispatcher {
    #private;
    constructor(options: ThreadGroupDispatcherConfig);
    name(tid: number): string | undefined;
    resolve(ph: string | undefined, tid: ThreadID | undefined): number | Error;
}
