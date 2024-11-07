"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getHierarchy = void 0;
function getHierarchy(instance) {
    const hierarchy = [];
    let currentClass = instance?.constructor;
    while (typeof currentClass === 'function') {
        hierarchy.push(currentClass);
        currentClass = Object.getPrototypeOf(currentClass);
    }
    return hierarchy.reverse();
}
exports.getHierarchy = getHierarchy;
//# sourceMappingURL=getHierarchy.js.map