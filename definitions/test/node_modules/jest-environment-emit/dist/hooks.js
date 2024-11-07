"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getEmitter = exports.registerSubscription = exports.onTestEnvironmentTeardown = exports.onHandleTestEvent = exports.onTestEnvironmentSetup = exports.onTestEnvironmentCreate = void 0;
const callbacks_1 = require("./callbacks");
const emitters_1 = require("./emitters");
const utils_1 = require("./utils");
const contexts = new WeakMap();
const staticListeners = new WeakMap();
function onTestEnvironmentCreate(jestEnvironment, jestEnvironmentConfig, environmentContext) {
    const testEvents = new emitters_1.SemiAsyncEmitter('jest-environment-emit', [
        'start_describe_definition',
        'finish_describe_definition',
        'add_hook',
        'add_test',
        'error',
    ]);
    const environmentConfig = normalizeJestEnvironmentConfig(jestEnvironmentConfig);
    utils_1.debugLogger.trace({
        testPath: environmentContext.testPath,
        environmentConfig,
    }, 'test_environment_create');
    contexts.set(jestEnvironment, {
        testEvents,
        environmentConfig,
        environmentContext,
    });
}
exports.onTestEnvironmentCreate = onTestEnvironmentCreate;
async function onTestEnvironmentSetup(env) {
    await subscribeToEvents(env);
    await getContext(env).testEvents.emit('test_environment_setup', {
        type: 'test_environment_setup',
        env,
    });
}
exports.onTestEnvironmentSetup = onTestEnvironmentSetup;
const onHandleTestEvent = (env, event, state) => getContext(env).testEvents.emit(event.name, { type: event.name, env, event, state });
exports.onHandleTestEvent = onHandleTestEvent;
async function onTestEnvironmentTeardown(env) {
    await getContext(env).testEvents.emit('test_environment_teardown', {
        type: 'test_environment_teardown',
        env,
    });
}
exports.onTestEnvironmentTeardown = onTestEnvironmentTeardown;
const registerSubscription = (klass, callback) => {
    const callbacks = staticListeners.get(klass) ?? [];
    callbacks.push(callback);
    staticListeners.set(klass, callbacks);
};
exports.registerSubscription = registerSubscription;
async function subscribeToEvents(env) {
    const envConfig = getContext(env).environmentConfig;
    const { projectConfig } = envConfig;
    const testEnvironmentOptions = projectConfig.testEnvironmentOptions;
    const staticRegistrations = collectStaticRegistrations(env);
    const configRegistrationsRaw = (testEnvironmentOptions.eventListeners ??
        []);
    const configRegistrations = await Promise.all(configRegistrationsRaw.map((r) => (0, callbacks_1.resolveSubscription)(projectConfig.rootDir, r)));
    const context = getCallbackContext(env);
    for (const [callback, options] of [...staticRegistrations, ...configRegistrations]) {
        await callback(context, options);
    }
}
function getContext(env) {
    const memo = contexts.get(env);
    if (!memo) {
        throw new Error('Environment context is not found. Most likely, you are using a non-valid environment reference.');
    }
    return memo;
}
function getEmitter(env) {
    return getContext(env).testEvents;
}
exports.getEmitter = getEmitter;
function getCallbackContext(env) {
    const memo = getContext(env);
    return Object.freeze({
        env,
        testEvents: memo.testEvents,
        context: memo.environmentContext,
        config: memo.environmentConfig,
    });
}
function normalizeJestEnvironmentConfig(jestEnvironmentConfig) {
    return jestEnvironmentConfig.globalConfig
        ? jestEnvironmentConfig
        : { projectConfig: jestEnvironmentConfig };
}
function collectStaticRegistrations(env) {
    return (0, utils_1.getHierarchy)(env)
        .flatMap((klass) => staticListeners.get(klass) ?? [])
        .map((callback) => [callback, void 0]);
}
//# sourceMappingURL=hooks.js.map