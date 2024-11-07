import { optimizeTracing } from '../utils';

const CATEGORIES = {
  ENQUEUE: ['enqueue'],
  EMIT: ['emit'],
  INVOKE: ['invoke'],
};

export const __ENQUEUE = optimizeTracing((_event: unknown) => ({
  cat: CATEGORIES.ENQUEUE,
}));

export const __EMIT = optimizeTracing((_event: unknown) => ({
  cat: CATEGORIES.EMIT,
}));

export const __INVOKE = optimizeTracing((listener: unknown, type?: '*') => ({
  cat: CATEGORIES.INVOKE,
  fn: `${listener}`,
  type,
}));
