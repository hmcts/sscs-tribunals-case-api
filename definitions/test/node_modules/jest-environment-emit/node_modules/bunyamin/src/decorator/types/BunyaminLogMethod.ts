import type { BunyaminLogRecordFields } from './BunyaminLogRecordFields';

export interface BunyaminLogMethod extends BunyaminLogMethodSignature {
  readonly begin: BunyaminLogMethodSignature;
  readonly complete: BunyaminCompleteMethodSignature;
  readonly end: BunyaminLogMethodSignature;
}

export interface BunyaminLogMethodSignature {
  (...message: unknown[]): void;
  (error: Error): void;
  (format: string, ...arguments_: unknown[]): void;
  (fields: BunyaminLogRecordFields, ...message: unknown[]): void;
  (fields: BunyaminLogRecordFields, format: string, ...arguments_: unknown[]): void;
}

export interface BunyaminCompleteMethodSignature {
  <T>(message: string, action: MaybeFunction<T>): T;
  <T>(event: BunyaminLogRecordFields, message: string, action: MaybeFunction<T>): T;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type MaybeFunction<T> = T | ((...arguments_: any[]) => T);
