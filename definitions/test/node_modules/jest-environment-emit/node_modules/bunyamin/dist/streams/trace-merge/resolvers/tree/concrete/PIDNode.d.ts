import { ArrayTreeNode } from '../abstract';
import { FileNode } from './FileNode';
export declare class PIDNode extends ArrayTreeNode<number, FileNode> {
    addFile(file: string): FileNode;
}
