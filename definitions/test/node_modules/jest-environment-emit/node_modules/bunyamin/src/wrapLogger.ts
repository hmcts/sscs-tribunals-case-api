import type { BunyaminConfig, BunyanLikeLogger } from './decorator';
import { Bunyamin } from './decorator';

export * from './decorator/types';
export type { Bunyamin } from './decorator';

export function wrapLogger<Logger extends BunyanLikeLogger>(
  options: BunyaminConfig<Logger>,
): Bunyamin<Logger>;
export function wrapLogger<Logger extends BunyanLikeLogger>(
  logger: Logger,
  options?: Omit<BunyaminConfig<Logger>, 'logger'>,
): Bunyamin<Logger>;
export function wrapLogger<Logger extends BunyanLikeLogger>(
  maybeLogger: any,
  maybeConfig?: unknown,
): Bunyamin<Logger> {
  const logger = (maybeLogger.logger ?? maybeLogger) as Logger;
  const config = (logger === maybeLogger ? maybeConfig : maybeLogger) as
    | BunyaminConfig<Logger>
    | undefined;

  return new Bunyamin({
    ...config,
    logger,
  });
}
