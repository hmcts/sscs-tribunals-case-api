import type { LeafNode, TreeNode } from './TreeNode';
export declare class RangeTreeNode<Value> implements LeafNode<Value> {
    #private;
    value: Value;
    index: number;
    parent?: TreeNode<any, this>;
    constructor(value: Value);
    get min(): number;
    get max(): number;
    get size(): number;
    protected add(child: number): void;
}
