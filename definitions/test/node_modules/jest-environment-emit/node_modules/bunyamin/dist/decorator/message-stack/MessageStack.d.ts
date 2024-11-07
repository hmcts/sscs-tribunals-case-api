import type { ThreadID } from '../../types';
export type MessageStackOptions = {
    noBeginMessage?: unknown;
};
export declare class MessageStack {
    #private;
    constructor(options?: MessageStackOptions);
    push(tid: ThreadID | undefined, message: unknown[]): void;
    pop(tid: ThreadID | undefined): unknown[];
}
