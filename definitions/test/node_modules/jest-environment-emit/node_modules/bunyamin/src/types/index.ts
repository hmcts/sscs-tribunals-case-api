import './typings.d';

export type ThreadID = ExplicitThreadID | ThreadAlias;
export type ExplicitThreadID = number;
export type ThreadAlias = SimpleThreadAlias | ComplexThreadAlias;
export type SimpleThreadAlias = string;
export type ComplexThreadAlias = [string, unknown];
