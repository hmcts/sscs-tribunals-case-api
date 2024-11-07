"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.compactObject = void 0;
/**
 * Mutates the given object by removing all keys with undefined values.
 */
function compactObject(maybeObject) {
    if (!maybeObject) {
        return maybeObject;
    }
    const object = maybeObject;
    for (const key of Object.keys(object)) {
        if (object[key] === undefined) {
            delete object[key];
        }
    }
    return maybeObject;
}
exports.compactObject = compactObject;
//# sourceMappingURL=compactObject.js.map