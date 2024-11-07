"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getProcessId = void 0;
/**
 * Simplistic cross-browser'ish get PID implementation
 * @returns {number} PID or 1 (stub)
 */
function getProcessId() {
    if (typeof process === 'object' && typeof process.pid === 'number') {
        return process.pid;
    }
    return 1;
}
exports.getProcessId = getProcessId;
//# sourceMappingURL=getProcessId.js.map