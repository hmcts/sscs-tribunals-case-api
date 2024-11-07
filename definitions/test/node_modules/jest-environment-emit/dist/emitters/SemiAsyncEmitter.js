"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SemiAsyncEmitter = void 0;
const utils_1 = require("../utils");
const SerialAsyncEmitter_1 = require("./SerialAsyncEmitter");
const SerialSyncEmitter_1 = require("./SerialSyncEmitter");
class SemiAsyncEmitter {
    #asyncEmitter;
    #syncEmitter;
    #syncEvents;
    constructor(name, syncEvents) {
        this.#asyncEmitter = new SerialAsyncEmitter_1.SerialAsyncEmitter(name);
        this.#syncEmitter = new SerialSyncEmitter_1.SerialSyncEmitter(name);
        this.#syncEvents = new Set(syncEvents);
    }
    on(type, listener, order) {
        (0, utils_1.assertString)(type, 'type');
        (0, utils_1.assertFunction)(listener, 'listener');
        order !== undefined && (0, utils_1.assertNumber)(order, 'order');
        return this.#invoke('on', type, listener, order);
    }
    once(type, listener, order) {
        (0, utils_1.assertString)(type, 'type');
        (0, utils_1.assertFunction)(listener, 'listener');
        order !== undefined && (0, utils_1.assertNumber)(order, 'order');
        return this.#invoke('once', type, listener, order);
    }
    off(type, listener) {
        (0, utils_1.assertString)(type, 'type');
        (0, utils_1.assertFunction)(listener, 'listener');
        return this.#invoke('off', type, listener);
    }
    emit(type, event) {
        (0, utils_1.assertString)(type, 'type');
        return this.#syncEvents.has(type)
            ? this.#syncEmitter.emit(type, event)
            : this.#asyncEmitter.emit(type, event);
    }
    #invoke(methodName, type, listener, order) {
        const isSync = this.#syncEvents.has(type);
        if (type === '*' || isSync) {
            this.#syncEmitter[methodName](type, listener, order);
        }
        if (type === '*' || !isSync) {
            this.#asyncEmitter[methodName](type, listener, order);
        }
        return this;
    }
}
exports.SemiAsyncEmitter = SemiAsyncEmitter;
//# sourceMappingURL=SemiAsyncEmitter.js.map