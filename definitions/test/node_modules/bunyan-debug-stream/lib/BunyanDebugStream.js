"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.create = exports.stdStringifiers = exports.serializers = void 0;
const bunyan_1 = __importDefault(require("bunyan"));
const chalk_1 = __importDefault(require("chalk"));
const path_1 = __importDefault(require("path"));
const stream_1 = require("stream");
const exceptionUtils_1 = require("./exceptionUtils");
const utils_1 = require("./utils");
// A list of various properties for the different bunyan levels.
const LEVELS = (function () {
    const answer = {};
    const o = (level, prefix, colors) => (answer[level] = { level, prefix, colors });
    o(bunyan_1.default.TRACE, 'TRACE:', ['grey']);
    o(bunyan_1.default.DEBUG, 'DEBUG:', ['cyan']);
    o(bunyan_1.default.INFO, 'INFO: ', ['green']);
    o(bunyan_1.default.WARN, 'WARN: ', ['yellow']);
    o(bunyan_1.default.ERROR, 'ERROR:', ['red']);
    o(bunyan_1.default.FATAL, 'FATAL:', ['magenta']);
    return answer;
})();
// A list of fields to not print, either because they are boring or because we explicitly pull them
// out and format them in some special way.
const FIELDS_TO_IGNORE = ['src', 'msg', 'name', 'hostname', 'pid', 'level', 'time', 'v', 'err'];
// express-bunyan-logger adds a bunch of fields to the `req`, and we don't wnat to print them all.
const EXPRESS_BUNYAN_LOGGER_FIELDS = [
    'remote-address',
    'ip',
    'method',
    'url',
    'referer',
    'user-agent',
    'body',
    'short-body',
    'http-version',
    'response-hrtime',
    'status-code',
    'req-headers',
    'res-headers',
    'incoming',
    'req_id',
];
// This takes log entries from Bunyan, and pretty prints them to the console.
//
class BunyanDebugStream extends stream_1.Writable {
    //
    // * `options.colors` is a hash where keys are Bunyan log levels (e.g. `bunyan.DEBUG`) and values
    //   are an array of colors (e.g. `['magenta', 'bold']`.)  Uses the `colors` module to apply
    //   all colors to the message before logging.  You can also set `options.colors` to `false`
    //   to disable colors.
    // * `options.forceColor` will turn color on, even if not using a TTY output.
    // * `options.basepath` is the absolute path of the root of your project.  If you're creating
    //   this `BunyanDebugStream` from a file called `app.js` in the root of your project, then
    //   this should be `__dirname`.
    // * `options.basepathReplacement` is a string to replace `options.basepath` with in filenames.
    //   Defaults to '.'.
    // * `options.showProcess` if true then will show "processName loggerName[pid]" in the output.
    //   If false (the default) then this will just be "loggerName[pid]".
    // * `options.showDate` if true, then show the date.  This assumes that `entry.time` is a `Date`
    //   object.  If false, time will not be shown.  You can also supply a `fn(time, entry)` here,
    //   which will be called to generate a date string if you want to customize the output
    //   format (or if `entry.time` is not a Date object).
    // * `options.showPrefixes` if false, then hide the prefixes. True by default.
    //   You can also supply a `fn(prefixesArray)` here, which will be called to generate a
    //   prefix string before the beginning of the `msg`. By default, it generates "[p1,...,pN]"
    //   string, i.e. square brackets surrounding comma-separated values.
    // * `options.processName` is the name of this process.  Defaults to the filename of the second
    //   argument in `process.argv` (on the assumption that you're running something like
    //   `node myApp.js`.)
    // * `options.maxExceptionLines` is the maximum number of lines to show in a stack trace.
    // * `options.stringifiers` is similar to Bunyan's serializers, but will be used to turn
    //   properties in log entries into strings.  A `null` stringifier can be used to hide a
    //   property from the logs.
    // * `options.prefixers` is similar to `options.stringifiers` but these strings will be prefixed
    //   onto the beginning of the `msg`, and wrapped in "[]" by default (use `options.showPrefixes`
    //   to customize the output).
    // * `options.out` is the stream to write data to.  Defaults to `process.stdout`.
    //
    constructor(options = {}) {
        var _a, _b, _c;
        super({ objectMode: true });
        this.options = options;
        // Enable colors for non-tty stdout
        if (this.options.forceColor) {
            chalk_1.default.level = 1;
        }
        // Compile color options
        this._colors = {};
        if (this.options.colors === false || this.options.colors === null) {
            // B&W for us.
            this._useColor = false;
            for (const levelValue of Object.keys(LEVELS)) {
                this._colors[levelValue] = [];
            }
        }
        else {
            this._useColor = true;
            // Load up the default colors
            for (const levelValue of Object.keys(LEVELS)) {
                const level = LEVELS[levelValue];
                this._colors[levelValue] = level.colors;
            }
            // Add in any colors from the options.
            const object = this.options.colors != null ? this.options.colors : {};
            for (const level of Object.keys(object)) {
                let c = object[level];
                if (typeof c === 'string') {
                    c = [c];
                }
                if (this._colors[level] != null) {
                    this._colors[level] = c;
                }
                else {
                    const levelNumber = bunyan_1.default[level.toUpperCase()];
                    if (this._colors[levelNumber] != null) {
                        this._colors[levelNumber] = c;
                    }
                    else {
                        // I don't know what to do with this...
                    }
                }
            }
        }
        this._processName =
            (_c = (_b = (_a = this.options.processName) !== null && _a !== void 0 ? _a : (process.argv.length > 1
                ? path_1.default.basename(process.argv[1], path_1.default.extname(process.argv[1]))
                : undefined)) !== null && _b !== void 0 ? _b : (process.argv.length > 0
                ? path_1.default.basename(process.argv[0], path_1.default.extname(process.argv[0]))
                : undefined)) !== null && _c !== void 0 ? _c : '';
        this._stringifiers = {
            req: exports.stdStringifiers.req,
            err: exports.stdStringifiers.err,
        };
        if (this.options.stringifiers != null) {
            for (const key of Object.keys(this.options.stringifiers)) {
                const value = this.options.stringifiers[key];
                this._stringifiers[key] = value;
            }
        }
        // Initialize some defaults
        this._prefixers = this.options.prefixers || {};
        this._out = this.options.out || process.stdout;
        this._basepath = this.options.basepath != null ? this.options.basepath : process.cwd();
        this._indent = this.options.indent != null ? this.options.indent : '  ';
        this._showDate = this.options.showDate != null ? this.options.showDate : true;
        this._showPrefixes = this.options.showPrefixes != null ? this.options.showPrefixes : true;
        this._showLoggerName =
            this.options.showLoggerName != null ? this.options.showLoggerName : true;
        this._showPid = this.options.showPid != null ? this.options.showPid : true;
        this._showLevel = this.options.showLevel != null ? this.options.showLevel : true;
        this._showMetadata = this.options.showMetadata != null ? this.options.showMetadata : true;
    }
    // Runs a stringifier.
    // Appends any keys consumed to `consumed`.
    //
    // Returns `{value, message}`.  If the `stringifier` returns `repalceMessage = true`, then
    // `value` will be null and `message` will be the result of the stringifier.  Otherwise
    // `message` will be the `message` passed in, and `value` will be the result of the stringifier.
    //
    _runStringifier(entry, key, stringifier, consumed, message) {
        consumed[key] = true;
        let value = null;
        let newMessage = message;
        try {
            if (stringifier == null) {
                // Null stringifier means we hide the value
            }
            else {
                const result = stringifier(entry[key], {
                    entry,
                    useColor: this._useColor,
                    debugStream: this,
                });
                if (result == null) {
                    // Hide the value
                }
                else if (typeof result === 'string') {
                    value = result;
                }
                else {
                    for (key of result.consumed != null ? result.consumed : []) {
                        consumed[key] = true;
                    }
                    if (result.value != null) {
                        if (result.replaceMessage) {
                            newMessage = result.value;
                            value = null;
                        }
                        else {
                            ({ value } = result);
                        }
                    }
                }
            }
        }
        catch (err) {
            // Go back to the original message
            newMessage = message;
            value = 'Error running stringifier:\n' + err.stack;
        }
        // Indent the result correctly
        if (value != null) {
            value = value.replace(/\n/g, `\n${this._indent}`);
        }
        return { message: newMessage, value };
    }
    _entryToString(entry) {
        let key, value;
        if (typeof entry === 'string') {
            entry = JSON.parse(entry);
        }
        const colorsToApply = this._colors[entry.level != null ? entry.level : bunyan_1.default.INFO];
        // src is the filename/line number
        let src = (0, utils_1.srcToString)(entry.src, this._basepath, this.options.basepathReplacement);
        if (src) {
            src += ': ';
        }
        let message = entry.msg;
        const consumed = {};
        for (const field of FIELDS_TO_IGNORE) {
            consumed[field] = true;
        }
        // Run our stringifiers
        const values = [];
        for (const key of Object.keys(this._stringifiers)) {
            const stringifier = this._stringifiers[key];
            if (entry[key] != null) {
                ({ message, value } = message = this._runStringifier(entry, key, stringifier, consumed, message));
                if (value != null) {
                    values.push(`${this._indent}${key}: ${value}`);
                }
            }
            else {
                consumed[key] = true;
            }
        }
        // Run our prefixers
        const prefixes = [];
        for (key in this._prefixers) {
            const prefixer = this._prefixers[key];
            if (entry[key] != null) {
                ({ message, value } = this._runStringifier(entry, key, prefixer, consumed, message));
                if (value != null) {
                    prefixes.push(value);
                }
            }
            else {
                consumed[key] = true;
            }
        }
        if (this._showMetadata) {
            // Use JSON.stringify on whatever is left
            for (key in entry) {
                // Skip fields we don't care about
                value = entry[key];
                if (consumed[key]) {
                    continue;
                }
                let valueString = JSON.stringify(value);
                if (valueString != null) {
                    // Make sure value isn't too long.
                    const cols = process.stdout.columns;
                    const start = `${this._indent}${key}: `;
                    if (cols && valueString.length + start.length >= cols) {
                        valueString = valueString.slice(0, cols - 3 - start.length) + '...';
                    }
                    values.push(`${start}${valueString}`);
                }
            }
        }
        let joinedPrefixes = '';
        if (this._showPrefixes && prefixes.length > 0) {
            if (typeof this._showPrefixes === 'function') {
                joinedPrefixes = `${this._showPrefixes(prefixes)} `;
            }
            else {
                joinedPrefixes = `[${prefixes.join(',')}] `;
            }
        }
        let date = undefined;
        if (this._showDate && typeof this._showDate === 'function') {
            date = `${this._showDate(entry.time, entry)} `;
        }
        else if (this._showDate) {
            date = `${(0, utils_1.dateToString)(entry.time != null ? entry.time : new Date())} `;
        }
        else {
            date = '';
        }
        let processStr = '';
        if (this.options.showProcess) {
            processStr += this._processName;
        }
        if (this._showLoggerName) {
            processStr += entry.name;
        }
        if (this._showPid) {
            processStr += `[${entry.pid}]`;
        }
        if (processStr.length > 0) {
            processStr += ' ';
        }
        const levelPrefix = this._showLevel
            ? ((LEVELS[entry.level] != null ? LEVELS[entry.level].prefix : undefined) != null
                ? LEVELS[entry.level] != null
                    ? LEVELS[entry.level].prefix
                    : undefined
                : '      ') + ' '
            : '';
        let line = `${date}${processStr}${levelPrefix}${src}${joinedPrefixes}${(0, utils_1.applyColors)(message, colorsToApply)}`;
        if (values.length > 0) {
            line += '\n' + values.map((v) => (0, utils_1.applyColors)(v, colorsToApply)).join('\n');
        }
        return line;
    }
    _write(entry, _encoding, done) {
        this._out.write(this._entryToString(entry) + '\n');
        return done();
    }
}
// Build our custom versions of the standard Bunyan serializers.
exports.serializers = {};
for (const serializerName in bunyan_1.default.stdSerializers) {
    const serializer = bunyan_1.default.stdSerializers[serializerName];
    exports.serializers[serializerName] = serializer;
}
exports.serializers.req = function (req) {
    const answer = bunyan_1.default.stdSerializers.req(req);
    if (answer != null) {
        if (req.user != null) {
            answer.user = req != null ? req.user : undefined;
        }
    }
    return answer;
};
exports.serializers.res = function (res) {
    const answer = bunyan_1.default.stdSerializers.res(res);
    if (answer != null) {
        answer.headers = (res === null || res === void 0 ? void 0 : res.getHeaders) ? res.getHeaders() : res._headers;
        if (res.responseTime != null) {
            answer.responseTime = res.responseTime;
        }
    }
    return answer;
};
exports.stdStringifiers = {
    req(req, { entry, useColor }) {
        var _a, _b;
        let status;
        let consumed = ['req', 'res'];
        const { res } = entry;
        if (entry['status-code'] != null &&
            entry['method'] != null &&
            entry['url'] != null &&
            entry['res-headers'] != null) {
            // This is an entry from express-bunyan-logger.  Add all the fields to `consumed`
            // so we don't print them out.
            consumed = consumed.concat(EXPRESS_BUNYAN_LOGGER_FIELDS);
        }
        // Get the statusCode
        const statusCode = (res != null ? res.statusCode : undefined) != null
            ? res != null
                ? res.statusCode
                : undefined
            : entry['status-code'];
        if (statusCode != null) {
            status = `${statusCode}`;
            if (useColor) {
                const statusColor = statusCode < 200 ? chalk_1.default.grey : statusCode < 400 ? chalk_1.default.green : chalk_1.default.red;
                status = chalk_1.default.bold(statusColor(status));
            }
        }
        else {
            status = '';
        }
        // Get the response time
        let responseTime = (() => {
            if ((res != null ? res.responseTime : undefined) != null) {
                return res.responseTime;
            }
            else if (entry.duration != null) {
                // bunyan-middleware stores response time in 'duration'
                consumed.push('duration');
                return entry.duration;
            }
            else if (entry['response-time'] != null) {
                // express-bunyan-logger stores response time in 'response-time'
                consumed.push('response-time');
                return entry['response-time'];
            }
            else {
                return null;
            }
        })();
        if (responseTime != null) {
            responseTime = `${responseTime}ms`;
        }
        else {
            responseTime = '';
        }
        // Get the user
        let user = '';
        if (req.user) {
            user = `${req.user.username || req.user.name || req.user}@`;
        }
        else if (entry.user) {
            consumed.push('user');
            user = `${entry.user.username || entry.user.name || entry.user}@`;
        }
        // Get the content length
        let contentLength = ((_a = res === null || res === void 0 ? void 0 : res.headers) === null || _a === void 0 ? void 0 : _a['content-length']) || ((_b = entry === null || entry === void 0 ? void 0 : entry['res-headers']) === null || _b === void 0 ? void 0 : _b['content-length']);
        contentLength = contentLength != null ? `- ${contentLength} bytes` : '';
        const host = (req.headers != null ? req.headers.host : undefined) || null;
        const url = host != null ? `${host}${req.url}` : `${req.url}`;
        let fields = [req.method, user + url, status, responseTime, contentLength];
        fields = fields.filter((f) => !!f);
        const request = fields.join(' ');
        // If there's no message, then replace the message with the request
        const replaceMessage = !entry.msg || entry.msg === 'request finish'; // bunyan-middleware
        return { consumed, value: request, replaceMessage };
    },
    err(err, { useColor, debugStream }) {
        return (0, exceptionUtils_1.formatException)(err, {
            color: !!useColor,
            maxLines: (debugStream.options != null
                ? debugStream.options.maxExceptionLines
                : undefined) !== undefined
                ? debugStream.options != null
                    ? debugStream.options.maxExceptionLines
                    : undefined
                : undefined,
            basePath: debugStream._basepath,
            basePathReplacement: debugStream.options != null ? debugStream.options.basepathReplacement : undefined,
        });
    },
};
function create(options) {
    return new BunyanDebugStream(options);
}
exports.create = create;
exports.default = create;
//# sourceMappingURL=BunyanDebugStream.js.map