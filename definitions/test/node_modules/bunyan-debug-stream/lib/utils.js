"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.srcToString = exports.toShortFilename = exports.applyColors = exports.dateToString = exports.lpad = void 0;
const path_1 = __importDefault(require("path"));
const chalk_1 = __importDefault(require("chalk"));
const endsWith = (str, suffix) => str.slice(-suffix.length) === suffix;
function lpad(str, count, fill = ' ') {
    str = '' + str;
    while (str.length < count) {
        str = fill + str;
    }
    return str;
}
exports.lpad = lpad;
// Convert a `date` into a syslog style "Nov 6 10:30:21".
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
function dateToString(date) {
    if (!date) {
        return date;
    }
    else if (date instanceof Date) {
        const time = [
            lpad(date.getHours(), 2, '0'),
            lpad(date.getMinutes(), 2, '0'),
            lpad(date.getSeconds(), 2, '0'),
        ].join(':');
        return [MONTHS[date.getMonth()], date.getDate(), time].join(' ');
    }
    else {
        return '' + date;
    }
}
exports.dateToString = dateToString;
// Applies one or more colors to a message, and returns the colorized message.
function applyColors(message, colorList) {
    if (message == null) {
        return message;
    }
    const c = chalk_1.default;
    for (const color of colorList) {
        message = c[color](message);
    }
    return message;
}
exports.applyColors = applyColors;
// Transforms "/src/foo/bar.coffee" to "/s/f/bar".
// Transforms "/src/foo/index.coffee" to "/s/foo/".
function toShortFilename(filename, basepath = undefined, replacement = './') {
    let shortenIndex;
    if (basepath) {
        if (typeof basepath === 'string' && !endsWith(basepath, path_1.default.sep)) {
            basepath += path_1.default.sep;
        }
        filename = filename.replace(basepath, replacement);
    }
    const parts = filename.split(path_1.default.sep);
    let file = parts[parts.length - 1];
    const ext = path_1.default.extname(file);
    file = path_1.default.basename(file, ext);
    if (file === 'index') {
        shortenIndex = parts.length - 3;
        file = '';
    }
    else {
        shortenIndex = parts.length - 2;
    }
    // Strip the extension
    parts[parts.length - 1] = file;
    for (let index = 0; index < parts.length; index++) {
        if (index <= shortenIndex) {
            parts[index] = parts[index][0];
        }
    }
    return parts.join('/');
}
exports.toShortFilename = toShortFilename;
// Transforms a bunyan `src` object (a `{file, line, func}` object) into a human readable string.
function srcToString(src, basepath = undefined, replacement = './') {
    if (src == null) {
        return '';
    }
    const file = (src.file != null ? toShortFilename(src.file, basepath, replacement) : '') +
        (src.line != null ? `:${src.line}` : '');
    const answer = src.func != null && file
        ? `${src.func} (${file})`
        : src.func != null
            ? src.func
            : file
                ? file
                : '';
    return answer;
}
exports.srcToString = srcToString;
//# sourceMappingURL=utils.js.map