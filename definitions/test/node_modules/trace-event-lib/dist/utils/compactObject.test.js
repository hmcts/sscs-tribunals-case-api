"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const compactObject_1 = require("./compactObject");
describe('compactObject', () => {
    it('should mutate the original object', () => {
        const input = { b: 2, c: 3 };
        const result = (0, compactObject_1.compactObject)(input);
        expect(result).toBe(input);
    });
    it('should omit undefined properties', () => {
        const input = { a: undefined, b: 2, c: 3, d: undefined };
        const result = (0, compactObject_1.compactObject)(input);
        expect(result).toEqual({ b: 2, c: 3 });
    });
    it('should have a fallback for non-objects', () => {
        const result = (0, compactObject_1.compactObject)(undefined);
        expect(result).toBe(undefined);
    });
});
//# sourceMappingURL=compactObject.test.js.map