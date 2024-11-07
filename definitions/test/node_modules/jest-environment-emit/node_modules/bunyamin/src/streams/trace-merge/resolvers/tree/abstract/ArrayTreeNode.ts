import type { LeafNode, TreeNode } from './TreeNode';

export class ArrayTreeNode<Value = unknown, ChildNode extends LeafNode<any> = LeafNode>
  implements TreeNode<Value, ChildNode>
{
  index = -1;
  parent?: TreeNode<any, this>;

  readonly #children: ChildNode[] = [];
  readonly #map = new Map<unknown, ChildNode>();

  constructor(public value: Value) {}

  get size() {
    return this.#children.length;
  }

  [Symbol.iterator]() {
    return this.#children[Symbol.iterator]();
  }

  findByValue(value: unknown): ChildNode | undefined {
    return this.#map.get(value);
  }

  appendChild(node: ChildNode): ChildNode {
    node.index = this.size;
    node.parent = this as TreeNode;
    this.#children.push(node);
    this.#map.set(node.value, node);
    return node;
  }
}
