"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.formatException = void 0;
const chalk_1 = __importDefault(require("chalk"));
const stackTraceRegexp = /^( *)at (.*)\((.*)\)/;
const TRUNCATE_IF_NO_OWN_LINES = 5;
/**
 * Format an exception for console output.
 */
function formatException(err, options) {
    const color = options.color !== undefined && options.color !== null ? options.color : true;
    const basePathReplacement = options.basePathReplacement || '.';
    const { basePath, maxLines } = options;
    const errString = errToString(err);
    const lines = errString.split('\n');
    const headerLines = [];
    const traceLines = [];
    let readingHeader = true;
    let lastOwnLine = -1;
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const match = stackTraceRegexp.exec(line);
        // Dump anything at the top into the 'header'.
        if (!match && readingHeader) {
            headerLines.push(line);
            continue;
        }
        readingHeader = false;
        // Anything after the header we'll try to parse and work out if it is
        // "our code" or not.
        if (!match) {
            traceLines.push(line);
        }
        else {
            const [, indent, fnName, fileName] = match;
            const matchesBaseMatch = basePath && fileName.startsWith(basePath);
            let relativeFileName = fileName;
            if (basePath && matchesBaseMatch) {
                relativeFileName = basePathReplacement + fileName.slice(basePath.length);
                lastOwnLine = i;
            }
            let traceLine = `${indent}at ${fnName}(${relativeFileName})`;
            if (color && matchesBaseMatch) {
                traceLine = chalk_1.default.bold(traceLine);
            }
            traceLines.push(traceLine);
        }
    }
    // Truncate the trace if we have too many lines.
    if (basePath && maxLines === 'auto') {
        if (lastOwnLine === -1 && traceLines.length > TRUNCATE_IF_NO_OWN_LINES) {
            traceLines.length = TRUNCATE_IF_NO_OWN_LINES;
            traceLines.push('    [truncated]');
        }
        else if (traceLines.length > lastOwnLine) {
            traceLines.length = lastOwnLine;
            traceLines.push('    [truncated]');
        }
    }
    else if (typeof maxLines === 'number' && traceLines.length > maxLines) {
        traceLines.length = maxLines;
        traceLines.push('    [truncated]');
    }
    return headerLines.concat(traceLines).join('\n');
}
exports.formatException = formatException;
function errToString(err) {
    let errString;
    if (typeof err === 'string') {
        errString = err;
    }
    else {
        errString = err.stack || `${err}`;
    }
    return errString;
}
//# sourceMappingURL=exceptionUtils.js.map