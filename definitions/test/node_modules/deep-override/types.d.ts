declare function override<T, U>(target:T, source:U): T & U;
declare function override<T, U, V>(target:T, source1:U, source2: V): T & U & V;
declare namespace override {}
export = override;