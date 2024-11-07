"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.optimizeTracing = exports.debugLogger = exports.logger = exports.aggregateLogs = void 0;
const tslib_1 = require("tslib");
const fs = tslib_1.__importStar(require("node:fs"));
const path = tslib_1.__importStar(require("node:path"));
const bunyamin_1 = require("bunyamin");
const bunyan_1 = require("bunyan");
const bunyan_debug_stream_1 = tslib_1.__importDefault(require("bunyan-debug-stream"));
const noop_1 = require("./noop");
const logsDirectory = process.env.JEST_BUNYAMIN_DIR;
const LOG_PATTERN = /^jest-bunyamin\..*\.log$/;
const PACKAGE_NAME = 'jest-environment-emit';
function isTraceEnabled() {
    return !!logsDirectory;
}
function createLogFilePath() {
    const suffix = process.env.JEST_WORKER_ID ? `_${process.env.JEST_WORKER_ID}` : '';
    let counter = 0;
    let filePath = '';
    do {
        filePath = path.join(process.env.JEST_BUNYAMIN_DIR, `jest-bunyamin.${process.pid}${suffix}${counter-- || '-0'}.log`);
    } while (fs.existsSync(filePath));
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    return filePath;
}
function createBunyanImpl(traceEnabled) {
    const label = process.env.JEST_WORKER_ID ? `Worker ${process.env.JEST_WORKER_ID}` : 'Main';
    const bunyan = (0, bunyan_1.createLogger)({
        name: `jest (${label})`,
        streams: [
            {
                type: 'raw',
                level: 'warn',
                stream: (0, bunyan_debug_stream_1.default)({
                    out: process.stderr,
                    showMetadata: false,
                    showDate: false,
                    showPid: false,
                    showProcess: false,
                    showLoggerName: false,
                    showLevel: false,
                    prefixers: {
                        cat: (value) => String(value).split(',', 1)[0],
                    },
                }),
            },
            ...(traceEnabled
                ? [
                    {
                        type: 'raw',
                        level: 'trace',
                        stream: (0, bunyamin_1.traceEventStream)({
                            filePath: createLogFilePath(),
                            threadGroups: bunyamin_1.bunyamin.threadGroups,
                        }),
                    },
                ]
                : []),
        ],
    });
    return bunyan;
}
async function aggregateLogs() {
    const root = logsDirectory;
    if (!root) {
        return;
    }
    const unitedLogPath = path.join(root, 'jest-bunyamin.log');
    if (fs.existsSync(unitedLogPath)) {
        fs.rmSync(unitedLogPath);
    }
    const logs = fs
        .readdirSync(root)
        .filter((x) => LOG_PATTERN.test(x))
        .map((x) => path.join(root, x));
    if (logs.length > 1) {
        await (0, bunyamin_1.uniteTraceEventsToFile)(logs, unitedLogPath);
        for (const x of logs)
            fs.rmSync(x);
    }
    else {
        fs.renameSync(logs[0], unitedLogPath);
    }
}
exports.aggregateLogs = aggregateLogs;
bunyamin_1.threadGroups.add({
    id: PACKAGE_NAME,
    displayName: PACKAGE_NAME,
});
bunyamin_1.bunyamin.useLogger(createBunyanImpl(isTraceEnabled()), 1);
exports.logger = bunyamin_1.bunyamin.child({
    cat: PACKAGE_NAME,
});
const isDebugMode = (0, bunyamin_1.isDebug)(PACKAGE_NAME);
exports.debugLogger = isDebugMode ? exports.logger : bunyamin_1.nobunyamin;
exports.optimizeTracing = isDebugMode ? (f) => f : (() => noop_1.noop);
//# sourceMappingURL=logger.js.map