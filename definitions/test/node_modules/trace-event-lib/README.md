# trace-event-lib

[![npm version](https://badge.fury.io/js/trace-event-lib.svg)](https://badge.fury.io/js/trace-event-lib)
[![CI](https://github.com/wix-incubator/trace-event-lib/actions/workflows/ci.yml/badge.svg)](https://github.com/wix-incubator/trace-event-lib/actions/workflows/ci.yml)
[![semantic-release: angular](https://img.shields.io/badge/semantic--release-angular-e10079?logo=semantic-release)](https://github.com/semantic-release/semantic-release)
[![Commitizen friendly](https://img.shields.io/badge/commitizen-friendly-brightgreen.svg)](http://commitizen.github.io/cz-cli/)

A library to create a trace of your JS app per [Google's Trace Event format](https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU).

![chrome://tracing example](https://raw.githubusercontent.com/wix-incubator/trace-event-lib/master/media/duration-events-test-ts-multiple-threads-2-snap.png)

These logs can then be visualized with:

* <chrome://tracing> (see the [archive](https://github.com/catapult-project/catapult/tree/master/tracing))
* [Perfetto](https://ui.perfetto.dev) – doesn't support async events, as of 29.06.2022.

# Install

```shell
npm install trace-event-lib --save
````

# Usage

```javascript
import { AbstractEventBuilder } from 'chrome-trace-event';

class ConcreteEventBuilder extends AbstractEventBuilder {
    send(event) {
        // Implement the abstract method: push events into a stream, array, etc.
    }
}

const trace = new ConcreteEventBuilder();

trace.begin({ cat: 'category1,category2', name: 'duration event' });
// ...
trace.instant({ name: 'resolve config', args: { /* ... */ } });
// ...
trace.complete({ name: 'nested event', dur: 3e6 /* 3s */ });
// ...
trace.end();

/**
 * Also, see the other methods on the website.
 *
 * @see {@link AbstractEventBuilder#beginAsync}
 * @see {@link AbstractEventBuilder#instantAsync}
 * @see {@link AbstractEventBuilder#endAsync}
 * @see {@link AbstractEventBuilder#counter}
 * @see {@link AbstractEventBuilder#metadata}
 * @see {@link AbstractEventBuilder#process_name}
 * @see {@link AbstractEventBuilder#process_labels}
 * @see {@link AbstractEventBuilder#process_sort_index}
 * @see {@link AbstractEventBuilder#thread_name}
 * @see {@link AbstractEventBuilder#thread_sort_index}
 */
```

## Links

* GH pages: <https://wix-incubator.github.io/trace-event-lib>
* Chrome Trace Event format specification: <https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU>
* Catapult project Wiki (archived): <https://github.com/google/trace-viewer/wiki>

## License

[MIT License](LICENSE)
