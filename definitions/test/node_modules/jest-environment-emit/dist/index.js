"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const hooks_1 = require("./hooks");
tslib_1.__exportStar(require("./types"), exports);
function WithEmitter(JestEnvironmentClass, callback, MixinName = 'WithEmitter') {
    const BaseClassName = JestEnvironmentClass.name;
    const CompositeClassName = `${MixinName}(${BaseClassName})`;
    const ClassWithEmitter = {
        [`${CompositeClassName}`]: class extends JestEnvironmentClass {
            testEvents;
            constructor(...args) {
                super(...args);
                (0, hooks_1.onTestEnvironmentCreate)(this, args[0], args[1]);
                this.testEvents = (0, hooks_1.getEmitter)(this);
            }
            static derive(callback, DerivedMixinName = MixinName) {
                const CurrentClass = this;
                const derivedName = `${DerivedMixinName}(${BaseClassName})`;
                const resultClass = {
                    [`${derivedName}`]: class extends CurrentClass {
                    },
                }[derivedName];
                (0, hooks_1.registerSubscription)(resultClass, callback);
                return resultClass;
            }
            async setup() {
                await super.setup?.();
                await (0, hooks_1.onTestEnvironmentSetup)(this);
            }
            handleTestEvent(event, state) {
                const maybePromise = super.handleTestEvent?.(event, state);
                return typeof maybePromise?.then === 'function'
                    ? maybePromise.then(() => (0, hooks_1.onHandleTestEvent)(this, event, state))
                    : (0, hooks_1.onHandleTestEvent)(this, event, state);
            }
            async teardown() {
                await super.teardown?.();
                await (0, hooks_1.onTestEnvironmentTeardown)(this);
            }
        },
    }[CompositeClassName];
    if (callback) {
        (0, hooks_1.registerSubscription)(ClassWithEmitter, callback);
    }
    return ClassWithEmitter;
}
exports.default = WithEmitter;
//# sourceMappingURL=index.js.map