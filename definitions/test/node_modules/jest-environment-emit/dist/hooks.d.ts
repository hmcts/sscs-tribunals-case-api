import type { EnvironmentContext, JestEnvironment, JestEnvironmentConfig } from '@jest/environment';
import type { Circus } from '@jest/types';
import type { EnvironmentListenerFn, EnvironmentEventEmitter } from './types';
export declare function onTestEnvironmentCreate(jestEnvironment: JestEnvironment, jestEnvironmentConfig: JestEnvironmentConfig, environmentContext: EnvironmentContext): void;
export declare function onTestEnvironmentSetup(env: JestEnvironment): Promise<void>;
export declare const onHandleTestEvent: (env: JestEnvironment, event: Circus.Event, state: Circus.State) => void | Promise<void>;
export declare function onTestEnvironmentTeardown(env: JestEnvironment): Promise<void>;
export declare const registerSubscription: <E extends JestEnvironment<unknown>>(klass: Function, callback: EnvironmentListenerFn<E>) => void;
export declare function getEmitter(env: JestEnvironment): EnvironmentEventEmitter;
