import hrtime from 'browser-process-hrtime';

/**
 * @returns {number} current time in microseconds
 */
export function now(): number {
  if (Number.isNaN(HRTIME_ZERO)) {
    HRTIME_ZERO = Date.now() - Math.round(hrtime2ms());
  }

  return HRTIME_ZERO + Math.round(hrtime2ms() * 1e3);
}

/** Starting point to align the human world time and OS-specific high-resolution time. */
let HRTIME_ZERO = Number.NaN;

/** Returns high-resolution time in milliseconds. */
function hrtime2ms() {
  const [seconds, nanoseconds] = hrtime();
  return s2ms(seconds) + ns2ms(nanoseconds);
}

/**
 * @param {number} s - seconds
 * @returns {number} microseconds
 */
function s2ms(s: number): number {
  return s * 1e6;
}

/**
 * @param {number} ns - nanoseconds
 * @returns {number} milliseconds
 */
function ns2ms(ns: number): number {
  return Math.round(ns * 1e-6);
}
