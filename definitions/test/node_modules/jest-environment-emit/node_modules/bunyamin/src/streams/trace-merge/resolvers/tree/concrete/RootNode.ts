import { ArrayTreeNode } from '../abstract';
import { PIDNode } from './PIDNode';

export class RootNode extends ArrayTreeNode<never, PIDNode> {
  constructor() {
    super(undefined as never);
  }

  addPID(pid: number): PIDNode {
    /* eslint-disable-next-line unicorn/prefer-dom-node-append */
    return this.findByValue(pid) ?? this.appendChild(new PIDNode(pid));
  }

  rank() {
    let index = 0;
    let offset = 0;

    for (const pid of this) {
      for (const file of pid) {
        file.rank = index++;
        file.offset = offset;
        offset += file.size;
      }
    }
  }
}
