"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.__INVOKE = exports.__EMIT = exports.__ENQUEUE = void 0;
const utils_1 = require("../utils");
const CATEGORIES = {
    ENQUEUE: ['enqueue'],
    EMIT: ['emit'],
    INVOKE: ['invoke'],
};
exports.__ENQUEUE = (0, utils_1.optimizeTracing)((_event) => ({
    cat: CATEGORIES.ENQUEUE,
}));
exports.__EMIT = (0, utils_1.optimizeTracing)((_event) => ({
    cat: CATEGORIES.EMIT,
}));
exports.__INVOKE = (0, utils_1.optimizeTracing)((listener, type) => ({
    cat: CATEGORIES.INVOKE,
    fn: `${listener}`,
    type,
}));
//# sourceMappingURL=syncEmitterCommons.js.map