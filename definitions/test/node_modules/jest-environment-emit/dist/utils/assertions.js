"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.assertNumber = exports.assertFunction = exports.assertString = void 0;
function assertString(value, name) {
    assertType('string', value, name);
}
exports.assertString = assertString;
function assertFunction(value, name) {
    assertType('function', value, name);
}
exports.assertFunction = assertFunction;
function assertNumber(value, name) {
    assertType('number', value, name);
}
exports.assertNumber = assertNumber;
function assertType(type, value, name) {
    if (typeof value !== type) {
        throw new TypeError(`${name} must be a ${type}`);
    }
}
//# sourceMappingURL=assertions.js.map