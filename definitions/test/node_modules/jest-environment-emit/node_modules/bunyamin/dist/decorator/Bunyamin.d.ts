import type { ThreadGroupConfig } from '../streams';
import type { BunyaminConfig, BunyaminLogMethod, BunyaminLogRecordFields as UserFields, BunyanLikeLogger } from './types';
export declare class Bunyamin<Logger extends BunyanLikeLogger = BunyanLikeLogger> {
    #private;
    readonly fatal: BunyaminLogMethod;
    readonly error: BunyaminLogMethod;
    readonly warn: BunyaminLogMethod;
    readonly info: BunyaminLogMethod;
    readonly debug: BunyaminLogMethod;
    readonly trace: BunyaminLogMethod;
    constructor(config: BunyaminConfig<Logger>, fields?: never);
    get threadGroups(): ThreadGroupConfig[];
    get logger(): Logger;
    set logger(logger: Logger);
    useLogger(logger: Logger, priority?: number): this;
    useTransform(transformFields: Required<BunyaminConfig<Logger>>['transformFields']): this;
    child(overrides?: UserFields): Bunyamin<Logger>;
}
