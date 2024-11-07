interface Result {
  equal: boolean;
  message: string;
}

export function setMatchingRules(rules: object, data: any, path: string): object;
export function getValue(data: any): object;
export function compare(actual: any, expected: any, rules: object, path: string, strict: boolean): Result;