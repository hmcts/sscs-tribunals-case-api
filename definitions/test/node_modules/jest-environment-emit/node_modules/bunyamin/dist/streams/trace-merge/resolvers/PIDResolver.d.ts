import { RootNode } from './tree';
import type { Resolver } from './Resolver';
export declare class PIDResolver implements Resolver {
    protected tree: RootNode;
    add(pid: number, _filePath: string, tid: number): void;
    finalize(): void;
    resolvePid(_filePath: string, pid: number): number;
    resolveTid(_filePath: string, pid: number, tid: number): number;
}
