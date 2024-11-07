declare const process: { pid?: number } | undefined;

/**
 * Simplistic cross-browser'ish get PID implementation
 * @returns {number} PID or 1 (stub)
 */
export function getProcessId(): number {
  if (typeof process === 'object' && typeof process.pid === 'number') {
    return process.pid;
  }

  return 1;
}
