"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.makeDeferred = void 0;
function makeDeferred() {
    let resolve;
    let reject;
    const promise = new Promise((_resolve, _reject) => {
        resolve = _resolve;
        reject = _reject;
    });
    return {
        promise: promise,
        resolve: resolve,
        reject: reject,
    };
}
exports.makeDeferred = makeDeferred;
//# sourceMappingURL=makeDeferred.js.map