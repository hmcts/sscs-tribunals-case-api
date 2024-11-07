# Node.js Logging

[![Greenkeeper badge](https://badges.greenkeeper.io/hmcts/nodejs-logging.svg)](https://greenkeeper.io/)

A logging component used by Reform's Node.js applications.

**This is not compatible with Reform tactical logging spec. Logger 2.x should be used for tactical applications.**

Some background info:
* there are 6 log levels: `silly` (5), `debug` (4), `verbose` (3), `info` (2), `warn` (1) and `error` (0).
* log level can be set via an environment variable `LOG_LEVEL`, the default is `info`.
* logging output in JSON format can be enabled by setting environment variable `JSON_PRINT` to `true`, the default is `false`:
* by default logging is turned off when running the unit tests.

## Usage

Add it as your project's dependency:

```bash
yarn add @hmcts/nodejs-logging
```

Require it:

```javascript
const { Logger } = require('@hmcts/nodejs-logging')
```

Then you can create a logger instance and use it to log information:

```javascript
const logger = Logger.getLogger('app.js') // app.js is just an example, can be anything that's meaningful to you
```

Usage are:

```
logger.info({
  message: 'Yay, logging!'
})
```

or

```
logger.log({
  level: 'info',
  message: 'What time is the testing at?'
});
```

Above will result in the following log printed (if JSON format is enabled).

```
{ level: 'info',
  message: 'What time is the testing at?',
  label: 'app.js',
  timestamp: '2017-09-30T03:57:26.875Z' }
```

### Access logging for Express applications 

Optionally you can use the built-in Express access logger:

```javascript
const { Express } = require('@hmcts/nodejs-logging')

app.use(Express.accessLogger())
```

## Units Tests

Just do

```
yarn test
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
