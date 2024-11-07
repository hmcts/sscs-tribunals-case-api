import { PIDResolver } from './PIDResolver';
export declare class FilePIDResolver extends PIDResolver {
    add(pid: number, filePath: string, tid: number): void;
    resolvePid(filePath: string, pid: number): number;
    resolveTid(filePath: string, pid: number, tid: number): number;
}
