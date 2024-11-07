"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.resolveSubscriptionSingle = exports.resolveSubscription = void 0;
const requireModule_1 = require("./requireModule");
async function resolveSubscription(rootDir, registration) {
    if (Array.isArray(registration)) {
        const [callback, options] = registration;
        return [await resolveSubscriptionSingle(rootDir, callback), options];
    }
    return [await resolveSubscriptionSingle(rootDir, registration), undefined];
}
exports.resolveSubscription = resolveSubscription;
async function resolveSubscriptionSingle(rootDir, registration) {
    if (typeof registration === 'function') {
        return registration;
    }
    if (typeof registration === 'string') {
        return resolveSubscriptionSingle(rootDir, await (0, requireModule_1.requireModule)(rootDir, registration));
    }
    return () => { };
}
exports.resolveSubscriptionSingle = resolveSubscriptionSingle;
//# sourceMappingURL=resolveSubscription.js.map