import type { BunyanLikeLogger } from '../decorator';
export declare class NoopLogger implements BunyanLikeLogger {
    fatal: any;
    error: any;
    warn: any;
    info: any;
    debug: any;
    trace: any;
}
export declare function noopLogger(_options?: any): NoopLogger;
