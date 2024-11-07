import type { EnvironmentContext, JestEnvironment, JestEnvironmentConfig } from '@jest/environment';
import type { Circus } from '@jest/types';
import type { ReadonlySemiAsyncEmitter } from './emitters';
export type TestEnvironmentEvent = TestEnvironmentSetupEvent | TestEnvironmentTeardownEvent | TestEnvironmentCircusEvent;
export type TestEnvironmentSetupEvent = {
    type: 'test_environment_setup';
    env: JestEnvironment;
};
export type TestEnvironmentTeardownEvent = {
    type: 'test_environment_teardown';
    env: JestEnvironment;
};
export type TestEnvironmentCircusEvent<E extends Circus.Event = Circus.Event> = {
    type: E['name'];
    env: JestEnvironment;
    event: E;
    state: Circus.State;
};
export type TestEnvironmentSyncEventMap = {
    add_hook: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'add_hook';
    }>;
    add_test: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'add_test';
    }>;
    error: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'error';
    }>;
    finish_describe_definition: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'finish_describe_definition';
    }>;
    start_describe_definition: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'start_describe_definition';
    }>;
};
export type TestEnvironmentAsyncEventMap = {
    hook_failure: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'hook_failure';
    }>;
    hook_start: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'hook_start';
    }>;
    hook_success: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'hook_success';
    }>;
    include_test_location_in_result: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'include_test_location_in_result';
    }>;
    run_describe_finish: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'run_describe_finish';
    }>;
    run_describe_start: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'run_describe_start';
    }>;
    run_finish: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'run_finish';
    }>;
    run_start: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'run_start';
    }>;
    setup: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'setup';
    }>;
    teardown: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'teardown';
    }>;
    test_done: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'test_done';
    }>;
    test_environment_setup: TestEnvironmentSetupEvent;
    test_environment_teardown: TestEnvironmentTeardownEvent;
    test_fn_failure: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'test_fn_failure';
    }>;
    test_fn_start: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'test_fn_start';
    }>;
    test_fn_success: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'test_fn_success';
    }>;
    test_retry: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'test_retry';
    }>;
    test_skip: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'test_skip';
    }>;
    test_start: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'test_start';
    }>;
    test_started: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'test_started';
    }>;
    test_todo: TestEnvironmentCircusEvent<Circus.Event & {
        name: 'test_todo';
    }>;
};
export type EnvironmentListener<E extends JestEnvironment = JestEnvironment> = EnvironmentListenerWithOptions<E> | EnvironmentListenerOnly<E>;
export type EnvironmentListenerWithOptions<E extends JestEnvironment = JestEnvironment> = [
    EnvironmentListenerOnly<E>,
    any
];
export type EnvironmentListenerOnly<E extends JestEnvironment = JestEnvironment> = EnvironmentListenerFn<E> | string;
export type EnvironmentListenerFn<E extends JestEnvironment = JestEnvironment> = (context: Readonly<EnvironmentListenerContext<E>>, listenerConfig?: any) => void;
export type EnvironmentEventEmitter = ReadonlySemiAsyncEmitter<TestEnvironmentAsyncEventMap, TestEnvironmentSyncEventMap>;
export type EnvironmentListenerContext<E extends JestEnvironment = JestEnvironment> = {
    env: E;
    testEvents: EnvironmentEventEmitter;
    context: EnvironmentContext;
    config: JestEnvironmentConfig;
};
