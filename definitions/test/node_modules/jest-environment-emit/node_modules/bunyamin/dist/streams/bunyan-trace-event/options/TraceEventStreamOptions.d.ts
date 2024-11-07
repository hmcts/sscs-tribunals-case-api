import type { ThreadGroupConfig } from '../threads';
export type TraceEventStreamOptions = {
    ignoreFields?: string[];
    threadGroups?: Iterable<string | ThreadGroupConfig>;
    maxConcurrency?: number;
    defaultThreadName?: string;
    strict?: boolean;
};
