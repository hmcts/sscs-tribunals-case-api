"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.now = void 0;
const browser_process_hrtime_1 = __importDefault(require("browser-process-hrtime"));
/**
 * @returns {number} current time in microseconds
 */
function now() {
    if (Number.isNaN(HRTIME_ZERO)) {
        HRTIME_ZERO = Date.now() - Math.round(hrtime2ms());
    }
    return HRTIME_ZERO + Math.round(hrtime2ms() * 1e3);
}
exports.now = now;
/** Starting point to align the human world time and OS-specific high-resolution time. */
let HRTIME_ZERO = Number.NaN;
/** Returns high-resolution time in milliseconds. */
function hrtime2ms() {
    const [seconds, nanoseconds] = (0, browser_process_hrtime_1.default)();
    return s2ms(seconds) + ns2ms(nanoseconds);
}
/**
 * @param {number} s - seconds
 * @returns {number} microseconds
 */
function s2ms(s) {
    return s * 1e6;
}
/**
 * @param {number} ns - nanoseconds
 * @returns {number} milliseconds
 */
function ns2ms(ns) {
    return Math.round(ns * 1e-6);
}
//# sourceMappingURL=now.js.map