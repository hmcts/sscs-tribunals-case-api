import type { JestEnvironment } from '@jest/environment';
import type {
  EnvironmentListener,
  EnvironmentListenerFn,
  EnvironmentListenerOnly,
  EnvironmentListenerWithOptions,
} from '../types';

import { requireModule } from './requireModule';

export type ResolvedEnvironmentListener<E extends JestEnvironment = JestEnvironment> = [
  EnvironmentListenerFn<E>,
  any,
];

export async function resolveSubscription<E extends JestEnvironment = JestEnvironment>(
  rootDir: string,
  registration: EnvironmentListener<E> | null,
): Promise<ResolvedEnvironmentListener<E>> {
  if (Array.isArray(registration)) {
    const [callback, options] = registration as EnvironmentListenerWithOptions<E>;
    return [await resolveSubscriptionSingle(rootDir, callback), options];
  }

  return [await resolveSubscriptionSingle(rootDir, registration), undefined];
}

export async function resolveSubscriptionSingle<E extends JestEnvironment = JestEnvironment>(
  rootDir: string,
  registration: EnvironmentListenerOnly<E> | null,
): Promise<EnvironmentListenerFn<E>> {
  if (typeof registration === 'function') {
    return registration;
  }

  if (typeof registration === 'string') {
    return resolveSubscriptionSingle(rootDir, await requireModule(rootDir, registration));
  }

  return () => {};
}
