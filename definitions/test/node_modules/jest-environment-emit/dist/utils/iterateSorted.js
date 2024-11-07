"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.iterateSorted = void 0;
function* iterateSorted(getIndex, a, b) {
    if (a === b) {
        yield* a;
        return;
    }
    const ia = a[Symbol.iterator]();
    const ib = b[Symbol.iterator]();
    let ea = ia.next();
    let eb = ib.next();
    while (!ea.done && !eb.done) {
        const va = ea.value;
        const vb = eb.value;
        const na = getIndex(va);
        const nb = getIndex(vb);
        if (na <= nb) {
            yield va;
            ea = ia.next();
        }
        else {
            yield vb;
            eb = ib.next();
        }
    }
    while (!ea.done) {
        yield ea.value;
        ea = ia.next();
    }
    while (!eb.done) {
        yield eb.value;
        eb = ib.next();
    }
}
exports.iterateSorted = iterateSorted;
//# sourceMappingURL=iterateSorted.js.map