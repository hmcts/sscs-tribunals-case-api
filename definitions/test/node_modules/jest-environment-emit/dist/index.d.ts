import type { JestEnvironment } from '@jest/environment';
import type { Circus } from '@jest/types';
import type { EnvironmentListener, EnvironmentListenerFn, EnvironmentEventEmitter } from './types';
export * from './types';
export default function WithEmitter<E extends JestEnvironment>(JestEnvironmentClass: new (...args: any[]) => E, callback?: EnvironmentListenerFn<E>, MixinName?: string): WithEmitterClass<E>;
export type WithTestEvents<E extends JestEnvironment> = E & {
    readonly testEvents: EnvironmentEventEmitter;
    handleTestEvent: Circus.EventHandler;
};
export type WithEmitterClass<E extends JestEnvironment> = (new (...args: any[]) => WithTestEvents<E>) & {
    derive(callback: EnvironmentListener<E>, ClassName?: string): WithEmitterClass<E>;
};
