import type { BunyaminConfig, BunyanLikeLogger } from './decorator';
import { Bunyamin } from './decorator';
export * from './decorator/types';
export type { Bunyamin } from './decorator';
export declare function wrapLogger<Logger extends BunyanLikeLogger>(options: BunyaminConfig<Logger>): Bunyamin<Logger>;
export declare function wrapLogger<Logger extends BunyanLikeLogger>(logger: Logger, options?: Omit<BunyaminConfig<Logger>, 'logger'>): Bunyamin<Logger>;
