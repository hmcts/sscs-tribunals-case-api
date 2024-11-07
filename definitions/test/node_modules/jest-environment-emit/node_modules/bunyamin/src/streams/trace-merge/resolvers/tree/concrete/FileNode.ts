import { RangeTreeNode } from '../abstract';

export class FileNode extends RangeTreeNode<string> {
  rank?: number;
  offset?: number;

  addTID(tid: number): void {
    return super.add(tid);
  }

  transpose(tid: number): number {
    if (tid < this.min || tid > this.max) {
      throw new Error(`Value ${tid} not found in range: [${this.min}, ${this.max}]`);
    }

    return (this.offset ?? 0) + (tid - this.min);
  }
}
