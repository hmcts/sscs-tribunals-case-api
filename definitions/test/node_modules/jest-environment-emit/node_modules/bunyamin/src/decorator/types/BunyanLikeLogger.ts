export type BunyanLikeLogger = {
  readonly fatal: BunyanLikeLogMethod;
  readonly error: BunyanLikeLogMethod;
  readonly warn: BunyanLikeLogMethod;
  readonly info: BunyanLikeLogMethod;
  readonly debug: BunyanLikeLogMethod;
  readonly trace: BunyanLikeLogMethod;
};

export interface BunyanLikeLogMethod {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (fields: object, ...message: any[]): void;
}

export type BunyanLogLevel = 'fatal' | 'error' | 'warn' | 'info' | 'debug' | 'trace';
