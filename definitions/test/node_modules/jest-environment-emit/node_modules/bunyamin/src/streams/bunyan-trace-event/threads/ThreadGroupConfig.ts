export type ThreadGroupConfig = {
  /**
   * Unique identifier of the thread group.
   */
  id: string;
  /**
   * Display name of the thread group in the trace viewer.
   */
  displayName: string;
  /**
   * Maximum number of concurrent threads in this group.
   * @default 100
   */
  maxConcurrency?: number;
};
