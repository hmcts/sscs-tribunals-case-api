"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SerialAsyncEmitter = void 0;
const logError_1 = require("./logError");
const ReadonlyEmitterBase_1 = require("./ReadonlyEmitterBase");
const syncEmitterCommons_1 = require("./syncEmitterCommons");
class SerialAsyncEmitter extends ReadonlyEmitterBase_1.ReadonlyEmitterBase {
    #idle;
    #tasks = [];
    emit(eventType, event) {
        this.#tasks.push(this.#doEmit(eventType, event));
        this.#idle ??= this.#waitForIdle();
        return this.#idle;
    }
    async #waitForIdle() {
        do {
            const $promises = new Set(this.#tasks);
            await Promise.all(this.#tasks);
            for (let index = this.#tasks.length - 1; index >= 0; index--) {
                if ($promises.has(this.#tasks[index])) {
                    this.#tasks.splice(index, 1);
                }
            }
        } while (this.#tasks.length > 0);
        this.#idle = undefined;
    }
    async #doEmit(eventType, event) {
        const listeners = [...this._getListeners(eventType)];
        const $eventType = String(eventType);
        await this._log.trace.complete((0, syncEmitterCommons_1.__EMIT)(event), $eventType, async () => {
            if (listeners) {
                for (const listener of listeners) {
                    try {
                        await this._log.trace.complete((0, syncEmitterCommons_1.__INVOKE)(listener), 'invoke', () => listener(event));
                    }
                    catch (error) {
                        (0, logError_1.logError)(error, $eventType, listener);
                    }
                }
            }
        });
    }
}
exports.SerialAsyncEmitter = SerialAsyncEmitter;
//# sourceMappingURL=SerialAsyncEmitter.js.map