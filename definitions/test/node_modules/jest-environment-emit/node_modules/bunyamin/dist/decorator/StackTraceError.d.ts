export declare class StackTraceError extends Error {
    constructor();
    static empty(): {
        message: string;
        stack: string;
    };
}
