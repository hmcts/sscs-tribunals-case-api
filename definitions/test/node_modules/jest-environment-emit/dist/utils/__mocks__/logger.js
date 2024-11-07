"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.optimizeTracing = exports.logger = void 0;
const { bunyamin } = jest.requireActual('bunyamin');
exports.logger = bunyamin;
bunyamin.useLogger({
    fatal: jest.fn(),
    error: jest.fn(),
    warn: jest.fn(),
    info: jest.fn(),
    debug: jest.fn(),
    trace: jest.fn(),
});
const optimizeTracing = jest.fn((f) => f);
exports.optimizeTracing = optimizeTracing;
//# sourceMappingURL=logger.js.map