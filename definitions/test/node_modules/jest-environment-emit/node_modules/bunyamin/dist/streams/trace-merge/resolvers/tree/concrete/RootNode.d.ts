import { ArrayTreeNode } from '../abstract';
import { PIDNode } from './PIDNode';
export declare class RootNode extends ArrayTreeNode<never, PIDNode> {
    constructor();
    addPID(pid: number): PIDNode;
    rank(): void;
}
