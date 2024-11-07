import type { LeafNode, TreeNode } from './TreeNode';
export declare class ArrayTreeNode<Value = unknown, ChildNode extends LeafNode<any> = LeafNode> implements TreeNode<Value, ChildNode> {
    #private;
    value: Value;
    index: number;
    parent?: TreeNode<any, this>;
    constructor(value: Value);
    get size(): number;
    [Symbol.iterator](): IterableIterator<ChildNode>;
    findByValue(value: unknown): ChildNode | undefined;
    appendChild(node: ChildNode): ChildNode;
}
