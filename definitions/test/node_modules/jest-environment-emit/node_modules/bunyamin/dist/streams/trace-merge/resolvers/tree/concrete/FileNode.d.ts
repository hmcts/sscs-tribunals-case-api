import { RangeTreeNode } from '../abstract';
export declare class FileNode extends RangeTreeNode<string> {
    rank?: number;
    offset?: number;
    addTID(tid: number): void;
    transpose(tid: number): number;
}
