export declare function aggregateLogs(): Promise<void>;
export declare const logger: import("bunyamin").Bunyamin<import("bunyamin").BunyanLikeLogger>;
export declare const debugLogger: import("bunyamin").Bunyamin<import("bunyamin").BunyanLikeLogger>;
export declare const optimizeTracing: <F>(f: F) => F;
