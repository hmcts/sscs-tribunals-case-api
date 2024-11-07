import type { JestEnvironment } from '@jest/environment';
import type { EnvironmentListener, EnvironmentListenerFn, EnvironmentListenerOnly } from '../types';
export type ResolvedEnvironmentListener<E extends JestEnvironment = JestEnvironment> = [
    EnvironmentListenerFn<E>,
    any
];
export declare function resolveSubscription<E extends JestEnvironment = JestEnvironment>(rootDir: string, registration: EnvironmentListener<E> | null): Promise<ResolvedEnvironmentListener<E>>;
export declare function resolveSubscriptionSingle<E extends JestEnvironment = JestEnvironment>(rootDir: string, registration: EnvironmentListenerOnly<E> | null): Promise<EnvironmentListenerFn<E>>;
