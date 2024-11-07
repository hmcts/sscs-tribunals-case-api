"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ReadonlyEmitterBase = void 0;
const utils_1 = require("../utils");
const __CATEGORY_LISTENERS = ['listeners'];
const __LISTENERS = (0, utils_1.optimizeTracing)((listener) => ({
    cat: __CATEGORY_LISTENERS,
    fn: `${listener}`,
}));
const ONCE = Symbol('ONCE');
class ReadonlyEmitterBase {
    _log;
    _listeners = new Map();
    #listenersCounter = 0;
    constructor(name) {
        this._log = utils_1.debugLogger.child({
            cat: `emitter`,
            tid: [name, {}],
        });
        this._listeners.set('*', []);
    }
    on(type, listener, order) {
        if (!listener[ONCE]) {
            this._log.trace(__LISTENERS(listener), 'on(%s)', type);
        }
        if (!this._listeners.has(type)) {
            this._listeners.set(type, []);
        }
        const listeners = this._listeners.get(type);
        listeners.push([listener, order ?? this.#listenersCounter++]);
        listeners.sort((a, b) => getOrder(a) - getOrder(b));
        return this;
    }
    once(type, listener, order) {
        this._log.trace(__LISTENERS(listener), 'once(%s)', type);
        return this.on(type, this.#createOnceListener(type, listener), order);
    }
    off(type, listener, _order) {
        if (!listener[ONCE]) {
            this._log.trace(__LISTENERS(listener), 'off(%s)', type);
        }
        const listeners = this._listeners.get(type) || [];
        const index = listeners.findIndex(([l]) => l === listener);
        if (index !== -1) {
            listeners.splice(index, 1);
        }
        return this;
    }
    *_getListeners(type) {
        const wildcard = this._listeners.get('*') ?? [];
        const named = this._listeners.get(type) ?? [];
        for (const [listener] of (0, utils_1.iterateSorted)(getOrder, wildcard, named)) {
            yield listener;
        }
    }
    #createOnceListener(type, listener) {
        const onceListener = ((event) => {
            this.off(type, onceListener);
            return listener(event);
        });
        onceListener.toString = listener.toString.bind(listener);
        onceListener[ONCE] = true;
        return onceListener;
    }
}
exports.ReadonlyEmitterBase = ReadonlyEmitterBase;
function getOrder([_a, b]) {
    return b;
}
//# sourceMappingURL=ReadonlyEmitterBase.js.map