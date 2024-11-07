import type { JestEnvironment } from '@jest/environment';
import type { Circus } from '@jest/types';

import {
  getEmitter,
  onHandleTestEvent,
  onTestEnvironmentCreate,
  onTestEnvironmentSetup,
  onTestEnvironmentTeardown,
  registerSubscription,
} from './hooks';

import type { EnvironmentListener, EnvironmentListenerFn, EnvironmentEventEmitter } from './types';

export * from './types';

/**
 * Decorator for a given JestEnvironment subclass that extends
 * {@link JestEnvironment#constructor}, {@link JestEnvironment#global},
 * {@link JestEnvironment#setup}, and {@link JestEnvironment#handleTestEvent}
 * and {@link JestEnvironment#teardown} in an extensible way.
 *
 * You won't need to extend this class directly – instead, you can tap into
 * the lifecycle events by registering listeners in `testEnvironmentOptions.eventListeners`
 * property in your Jest configuration file.
 *
 * Even a less intrusive way to tap into the lifecycle events is to use the
 * static `register` method, which accepts a callback that will be invoked
 * with the decorated Jest environment instance, the emitter, and the
 * environment context.
 *
 * @param JestEnvironmentClass - Jest environment subclass to decorate
 * @returns a decorated Jest environment subclass, e.g. `WithMetadata(JestEnvironmentNode)`
 * @example
 * ```javascript
 * import { WithEmitter } from 'jest-environment-emit';
 * import JestEnvironmentNode from 'jest-environment-node';
 *
 * export const TestEnvironment = WithEmitter(JestEnvironmentNode, {
 *   test_environment_setup: async () => {},
 *   test_environment_teardown: async () => {},
 *   setup: async () => {},
 *   teardown: async () => {},
 *   add_hook: () => {},
 *   add_test: () => {},
 *   error: () => {},
 *   test_fn_start: async () => {},
 *   test_fn_success: async () => {},
 *   test_fn_failure: async () => {},
 * }, 'WithMyListeners'); // > class WithMyListeners(JestEnvironmentNode) {}
 *
 * export const AdvancedTestEnvironment = TestEnvironment.derive('WithMoreListeners', {
 *   test_environment_setup: async () => {},
 * });
 */

export default function WithEmitter<E extends JestEnvironment>(
  JestEnvironmentClass: new (...args: any[]) => E,
  callback?: EnvironmentListenerFn<E>,
  MixinName = 'WithEmitter',
): WithEmitterClass<E> {
  const BaseClassName = JestEnvironmentClass.name;
  const CompositeClassName = `${MixinName}(${BaseClassName})`;
  const ClassWithEmitter = {
    // @ts-expect-error TS2415: Class '[`${compositeName}`]' incorrectly extends base class 'E'.
    [`${CompositeClassName}`]: class extends JestEnvironmentClass {
      readonly testEvents: EnvironmentEventEmitter;

      constructor(...args: any[]) {
        super(...args);
        onTestEnvironmentCreate(this, args[0], args[1]);
        this.testEvents = getEmitter(this);
      }

      static derive(
        callback: EnvironmentListenerFn<E>,
        DerivedMixinName = MixinName,
      ): WithEmitterClass<E> {
        const CurrentClass = this as unknown as WithEmitterClass<E>;
        const derivedName = `${DerivedMixinName}(${BaseClassName})`;
        const resultClass = {
          [`${derivedName}`]: class extends CurrentClass {},
        }[derivedName];
        registerSubscription(resultClass, callback);
        return resultClass;
      }

      async setup() {
        await super.setup?.();
        await onTestEnvironmentSetup(this);
      }

      // @ts-expect-error TS2415: The base class has an arrow function, but this can be a method
      handleTestEvent(event: Circus.Event, state: Circus.State): void | Promise<void> {
        const maybePromise = (super.handleTestEvent as JestEnvironment['handleTestEvent'])?.(
          event as any,
          state,
        );

        return typeof maybePromise?.then === 'function'
          ? maybePromise.then(() => onHandleTestEvent(this, event, state))
          : onHandleTestEvent(this, event, state);
      }

      async teardown() {
        await super.teardown?.();
        await onTestEnvironmentTeardown(this);
      }
    },
  }[CompositeClassName] as unknown as WithEmitterClass<E>;

  if (callback) {
    registerSubscription(ClassWithEmitter, callback);
  }

  return ClassWithEmitter;
}

export type WithTestEvents<E extends JestEnvironment> = E & {
  readonly testEvents: EnvironmentEventEmitter;
  handleTestEvent: Circus.EventHandler;
};

export type WithEmitterClass<E extends JestEnvironment> = (new (
  ...args: any[]
) => WithTestEvents<E>) & {
  derive(callback: EnvironmentListener<E>, ClassName?: string): WithEmitterClass<E>;
};
