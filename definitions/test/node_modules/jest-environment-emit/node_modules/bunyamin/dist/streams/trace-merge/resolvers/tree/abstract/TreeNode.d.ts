export interface TreeNode<Value = unknown, ChildNode extends LeafNode<any> = LeafNode> extends LeafNode<Value> {
    appendChild(value: ChildNode): ChildNode;
}
export interface LeafNode<Value = unknown> {
    value: Value;
    index: number;
    parent?: TreeNode<any, LeafNode<Value>>;
}
