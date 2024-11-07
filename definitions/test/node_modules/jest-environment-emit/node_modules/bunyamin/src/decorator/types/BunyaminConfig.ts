import type { ThreadGroupConfig } from '../../streams';
import type { BunyaminLogRecordFields } from './BunyaminLogRecordFields';
import type { BunyanLikeLogger } from './BunyanLikeLogger';

export type BunyaminConfig<Logger extends BunyanLikeLogger> = {
  /**
   * Underlying logger to be used.
   */
  logger: Logger;
  /**
   * Forbids changing the logger after constuction.
   */
  immutable?: boolean;
  /**
   * Thread groups to be used for grouping log records.
   */
  threadGroups?: Iterable<ThreadGroupConfig>;
  /**
   * Fallback message to be used when there was no previous message
   * passed with {@link BunyaminLogMethod#begin}.
   * @default '<no begin message>'
   */
  noBeginMessage?: string | unknown;
  /**
   * Optional transformation of log record fields provided by the user.
   */
  transformFields?: (
    fields: BunyaminLogRecordFields | undefined,
  ) => BunyaminLogRecordFields | undefined;
};
