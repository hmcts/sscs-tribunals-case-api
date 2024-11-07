import type { Bunyamin } from '../decorator';
import type { ThreadGroupConfig } from '../streams';
export declare class ThreadGroups implements Iterable<ThreadGroupConfig> {
    #private;
    constructor(getBunyamin: () => Bunyamin);
    add(group: ThreadGroupConfig): this;
    [Symbol.iterator](): IterableIterator<ThreadGroupConfig>;
}
