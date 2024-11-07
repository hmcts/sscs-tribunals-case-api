import type { LeafNode, TreeNode } from './TreeNode';

export class RangeTreeNode<Value> implements LeafNode<Value> {
  index = -1;
  parent?: TreeNode<any, this>;

  #min = Number.POSITIVE_INFINITY;
  #max = Number.NEGATIVE_INFINITY;

  constructor(public value: Value) {}

  get min() {
    return this.#min;
  }

  get max() {
    return this.#max;
  }

  get size() {
    return this.#max - this.#min + 1;
  }

  protected add(child: number) {
    if (child < this.#min) {
      this.#min = child;
    }
    if (child > this.#max) {
      this.#max = child;
    }
  }
}
