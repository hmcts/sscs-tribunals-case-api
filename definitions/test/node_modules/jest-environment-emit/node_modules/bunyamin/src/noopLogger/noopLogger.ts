import type { BunyanLikeLogger } from '../decorator';

const noop: any = () => {
  /* no-op */
};

export class NoopLogger implements BunyanLikeLogger {
  fatal = noop;
  error = noop;
  warn = noop;
  info = noop;
  debug = noop;
  trace = noop;
}

export function noopLogger(_options?: any) {
  return new NoopLogger();
}
