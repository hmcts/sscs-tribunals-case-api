declare module 'funpermaproxy' {
    const fp: FunpermaproxyFunction;

    interface FunpermaproxyFunction {
        <T>(getter: () => T): T;
        callable<T extends Function>(getter: () => T): T;
    }

    export = fp;
}
