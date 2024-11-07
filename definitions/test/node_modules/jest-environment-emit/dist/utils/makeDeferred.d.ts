export declare function makeDeferred<T>(): Deferred<T>;
export type Deferred<T> = {
    promise: Promise<T>;
    resolve: (value: T) => void;
    reject: (reason?: unknown) => void;
};
