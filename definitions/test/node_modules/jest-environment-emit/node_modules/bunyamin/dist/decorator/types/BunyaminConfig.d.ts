import type { ThreadGroupConfig } from '../../streams';
import type { BunyaminLogRecordFields } from './BunyaminLogRecordFields';
import type { BunyanLikeLogger } from './BunyanLikeLogger';
export type BunyaminConfig<Logger extends BunyanLikeLogger> = {
    logger: Logger;
    immutable?: boolean;
    threadGroups?: Iterable<ThreadGroupConfig>;
    noBeginMessage?: string | unknown;
    transformFields?: (fields: BunyaminLogRecordFields | undefined) => BunyaminLogRecordFields | undefined;
};
