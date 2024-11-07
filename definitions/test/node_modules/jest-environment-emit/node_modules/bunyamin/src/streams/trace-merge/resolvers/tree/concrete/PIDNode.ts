import { ArrayTreeNode } from '../abstract';
import { FileNode } from './FileNode';

export class PIDNode extends ArrayTreeNode<number, FileNode> {
  addFile(file: string): FileNode {
    /* eslint-disable-next-line unicorn/prefer-dom-node-append */
    return this.findByValue(file) ?? this.appendChild(new FileNode(file));
  }
}
