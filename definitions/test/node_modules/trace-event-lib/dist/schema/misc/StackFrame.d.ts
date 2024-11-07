/**
 * A stringified number
 * @example "5"
 */
export type StackFrameId = string;
/**
 * @example
 * { parent: "5", name: "SomeFunction", category: "my app" }
 */
export type StackFrame = {
    parent?: StackFrameId;
    name: string;
    category: string;
};
