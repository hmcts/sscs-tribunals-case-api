import { logger } from '../utils';

export function logError(error: unknown, eventType: unknown, listener: unknown) {
  logger.warn(
    error,
    `Caught an error while emitting %j event in a listener function:\n%s`,
    eventType,
    listener,
  );
}
