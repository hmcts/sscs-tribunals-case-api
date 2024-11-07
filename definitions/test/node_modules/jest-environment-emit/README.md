<div align="center">

![logo](https://github.com/wix-incubator/jest-environment-emit/assets/1962469/02006bb8-e7e4-45e1-9876-9b11316ed912)

## jest-environment-emit

_Environment with unlimited test event handlers_

[![npm version](https://badge.fury.io/js/jest-environment-emit.svg)](https://badge.fury.io/js/jest-environment-emit)

</div>

## Overview

There can be only one test environment per a project config in Jest unlike test reporters, which can be multiple.

This limitation is not convenient, so this package provides a couple of pre-defined test environments and a way to create custom ones:

* `jest-environment-emit` – exports `WithEmitter`, a higher-order function to create custom environment classes
* `jest-environment-emit/node` – exports `TestEnvironment`, a wrapper over `jest-environment-node`
* `jest-environment-emit/jsdom` – exports `TestEnvironment`, a wrapper over `jest-environment-jsdom`

To wrap a custom environment, use `WithEmitter`:

```js
import { WithEmitter } from 'jest-environment-emit';
// ...
export default WithEmitter(MyEnvironment);
```

## Usage

Assuming you have any test environment wrapped with `jest-environment-emit`, you can add as many event listeners as you want:

```js
/** @type {import('jest').Config} */
module.exports = {
  // ...
  testEnvironment: 'jest-environment-emit/node',
  testEnvironmentOptions: {
    eventListeners: [
      './simple-subscription-1.js',
      ['./parametrized-subscription-2.js', { some: 'options' }],
    ]
  },
};
```

The modules specified in `eventListeners` are required and should export a function with the following signature:

```js
/** @type {import('jest-environment-emit').EnvironmentListenerFn} */
const subscription = function (context, options) {
  context.testEvents
    .on('test_environment_setup', async ({ env }) => {
      // use like TestEnvironment#setup, e.g.:
      env.global.__SOME__ = 'value';
    })
    .on('add_hook', ({ event, state }) => {
      // use like TestEnvironment#handleTestEvent in jest-circus
    })
    .on('run_start', async ({ event, state }) => {
      // use like TestEnvironment#handleTestEvent in jest-circus
    })
    .on('test_environment_teardown', async ({ env }) => {
      // use like TestEnvironment#teardown
    })
    .on('*', ({ type }) => {
        // wildcard listener
        if (type === 'test_start') {
            // ...
        }
    });
};

export default subscription; // module.exports = subscription;
```

For exact type definitions, see [src/types.ts](src/types.ts).

## Library API

### Deriving from classes

This higher-order function can also accept one subscriber function and custom class name:

```js
import { WithEmitter } from 'jest-environment-emit';
// ...
export default WithEmitter(MyEnvironment, 'WithMyListeners', (context, options) => {
  context.testEvents.on('*', ({ type }) => {
    // ...
  });
}); // -> class WithMyListeners(MyEnvironment) { ... }
```

The derived classes have a static method `derive` to continue derivation, e.g.:

```js
import { TestEnvironment } from 'jest-environment-emit/node';

export default TestEnvironment.derive((context, options) => {
  context.testEvents.on('*', ({ type }) => {
    // ...
  });
}, 'WithMyListeners'); // -> class MyTestEnvironment(JestEnvironment) { ... }
```

All derived classes have also a protected property `testEvents` to access the event emitter.

```js
import { TestEnvironment } from 'jest-environment-emit/node';

export class MyTestEnvironment extends TestEnvironment {
  constructor(config, context) {
    super(config, context);

    this.testEvents.on('*', ({ type }) => {
      // ...
    });
  }
}
```

### Using custom priorities

By default, all event listeners are appended to the list. If you need to guarantee the order of execution, you can specify a priority:

```js
/** @type {import('jest-environment-emit').EnvironmentListenerFn} */
const subscription = function (context, options) {
  context.testEvents
    .on('test_environment_setup', async ({ env }) => {
      // make sure this listener is executed first
    }, -1)
    .on('test_environment_teardown', async ({ env }) => {
      // make sure this listener is executed last
    }, 1E6)
};
```

Try to avoid using priorities unless you really need them.

## Troubleshooting

Use `JEST_BUNYAMIN_DIR=path/to/dir` to enable debug logging.

In your `globalTeardown` script, you can aggregate all the logs into a single file for convenience:

```js
// globalTeardown.js
import { aggregateLogs } from 'jest-environment-emit/debug';

module.exports = async () => {
  await aggregateLogs();
};
```

The logs, e.g. `jest-bunyamin.log` is a file viewable with [Perfetto](https://ui.perfetto.dev/) or `chrome://tracing`.
