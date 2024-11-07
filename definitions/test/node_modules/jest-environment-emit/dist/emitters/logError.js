"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.logError = void 0;
const utils_1 = require("../utils");
function logError(error, eventType, listener) {
    utils_1.logger.warn(error, `Caught an error while emitting %j event in a listener function:\n%s`, eventType, listener);
}
exports.logError = logError;
//# sourceMappingURL=logError.js.map