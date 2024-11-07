export declare class ThreadDispatcher {
    #private;
    readonly name: string;
    readonly strict: boolean;
    readonly min: number;
    readonly max: number;
    constructor(name: string, strict: boolean, min: number, max: number);
    begin(id?: unknown): number | Error;
    resolve(id?: unknown): number | Error;
    end(id?: unknown): number | Error;
}
