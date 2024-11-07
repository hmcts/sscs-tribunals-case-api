"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.requireModule = void 0;
const utils_1 = require("../utils");
async function requireModule(rootDir, moduleName) {
    try {
        const cwdPath = require.resolve(moduleName, { paths: [rootDir] });
        return (await importModule(cwdPath));
    }
    catch (error) {
        utils_1.logger.warn({ cat: 'import', err: error }, `Failed to resolve: ${moduleName}`);
        return null;
    }
}
exports.requireModule = requireModule;
async function importModule(absolutePath) {
    let result = await import(absolutePath);
    if (result.__esModule) {
        result = result.default;
    }
    return result.default ?? result;
}
//# sourceMappingURL=requireModule.js.map