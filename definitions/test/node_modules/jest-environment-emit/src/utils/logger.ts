import * as fs from 'node:fs';
import * as path from 'node:path';
import {
  bunyamin,
  nobunyamin,
  isDebug,
  threadGroups,
  traceEventStream,
  uniteTraceEventsToFile,
} from 'bunyamin';
import { createLogger } from 'bunyan';
import createDebugStream from 'bunyan-debug-stream';
import { noop } from './noop';

const logsDirectory = process.env.JEST_BUNYAMIN_DIR;
const LOG_PATTERN = /^jest-bunyamin\..*\.log$/;
const PACKAGE_NAME = 'jest-environment-emit' as const;

function isTraceEnabled(): boolean {
  return !!logsDirectory;
}

function createLogFilePath() {
  const suffix = process.env.JEST_WORKER_ID ? `_${process.env.JEST_WORKER_ID}` : '';
  let counter = 0;
  let filePath = '';

  do {
    // modules are re-initialized for each test file,
    // so we cannot check once and cache the result
    filePath = path.join(
      process.env.JEST_BUNYAMIN_DIR!,
      `jest-bunyamin.${process.pid}${suffix}${counter-- || '-0'}.log`,
    );
  } while (fs.existsSync(filePath));

  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  return filePath;
}

function createBunyanImpl(traceEnabled: boolean) {
  const label = process.env.JEST_WORKER_ID ? `Worker ${process.env.JEST_WORKER_ID}` : 'Main';
  const bunyan = createLogger({
    name: `jest (${label})`,
    streams: [
      {
        type: 'raw',
        level: 'warn' as const,
        stream: createDebugStream({
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
              level: 'trace' as const,
              stream: traceEventStream({
                filePath: createLogFilePath(),
                threadGroups: bunyamin.threadGroups,
              }),
            },
          ]
        : []),
    ],
  });

  return bunyan;
}

export async function aggregateLogs() {
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
    await uniteTraceEventsToFile(logs, unitedLogPath);
    for (const x of logs) fs.rmSync(x);
  } else {
    fs.renameSync(logs[0], unitedLogPath);
  }
}

threadGroups.add({
  id: PACKAGE_NAME,
  displayName: PACKAGE_NAME,
});

bunyamin.useLogger(createBunyanImpl(isTraceEnabled()), 1);

export const logger = bunyamin.child({
  cat: PACKAGE_NAME,
});

const isDebugMode = isDebug(PACKAGE_NAME);

export const debugLogger = isDebugMode ? logger : nobunyamin;

export const optimizeTracing: <F>(f: F) => F = isDebugMode ? (f) => f : ((() => noop) as any);
