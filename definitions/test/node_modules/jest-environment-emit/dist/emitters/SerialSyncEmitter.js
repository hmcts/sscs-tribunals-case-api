"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SerialSyncEmitter = void 0;
const logError_1 = require("./logError");
const ReadonlyEmitterBase_1 = require("./ReadonlyEmitterBase");
const syncEmitterCommons_1 = require("./syncEmitterCommons");
class SerialSyncEmitter extends ReadonlyEmitterBase_1.ReadonlyEmitterBase {
    #queue = [];
    emit(nextEventType, nextEvent) {
        this.#queue.push([nextEventType, Object.freeze(nextEvent)]);
        if (this.#queue.length > 1) {
            this._log.trace((0, syncEmitterCommons_1.__ENQUEUE)(nextEvent), `enqueue(${String(nextEventType)})`);
            return;
        }
        while (this.#queue.length > 0) {
            const [eventType, event] = this.#queue[0];
            const listeners = [...this._getListeners(eventType)];
            const $eventType = String(eventType);
            this._log.trace.complete((0, syncEmitterCommons_1.__EMIT)(event), $eventType, () => {
                if (listeners) {
                    for (const listener of listeners) {
                        try {
                            this._log.trace.complete((0, syncEmitterCommons_1.__INVOKE)(listener), 'invoke', () => listener(event));
                        }
                        catch (error) {
                            (0, logError_1.logError)(error, $eventType, listener);
                        }
                    }
                }
            });
            this.#queue.shift();
        }
    }
}
exports.SerialSyncEmitter = SerialSyncEmitter;
//# sourceMappingURL=SerialSyncEmitter.js.map