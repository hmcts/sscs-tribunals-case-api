export interface Resolver {
    add(pid: number, filePath: string, tid: number): void;
    finalize(): void;
    resolvePid(filePath: string, pid: number): number;
    resolveTid(filePath: string, pid: number, tid: number): number;
}
