export * from './noopLogger';
export * from './traceEventStream';
export * from './uniteTraceEvents';
export * from './wrapLogger';
export { isDebug } from './is-debug';
export declare const bunyamin: import("./wrapLogger").Bunyamin<import("./wrapLogger").BunyanLikeLogger>;
export declare const nobunyamin: import("./wrapLogger").Bunyamin<import("./wrapLogger").BunyanLikeLogger>;
export declare const threadGroups: import("./thread-groups").ThreadGroups;
export default bunyamin;
