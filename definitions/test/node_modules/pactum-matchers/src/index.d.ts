export * as utils from './utils';

interface EachLikeOptions {
  min?: number;
  items?: any[];
}

/**
 * type matching
 */
export function like(value: any): object;

/**
 * array type matching 
 */
export function eachLike(content: any, options?: EachLikeOptions): object;

/**
 * oneOf matching
 */
export function oneOf(value: any): object;

/**
 * expression matching
 */
export function expression(value: any, expr: string): object;
export function expression(expr: string): object;

/**
 * non empty string matching
 */
export function string(value?: string): object;

/**
 * regex matching
 */
export function regex(value: any, matcher?: RegExp | string): object;

/**
 * partial regex matching
 */
export function includes(value: string | number): object;

/**
 * email matching
 */
export function email(value?: string): object;

/**
 * uuid matching
 */
export function uuid(value?: string): object;

/**
 * ISO 8601 date matching
 */
export function date(value?: string): object;

/**
 * ISO 8601 date-time matching
 */
export function dateTime(value?: string): object;

/**
 * Any type matching
 */
export function any(value?: any): object;

/**
 * Int matching
 */
export function int(value?: number): object;

/**
 * Float matching
 */
export function float(value?: number): object;

/**
 * Greater than given number matching
 */
export function gt(value?: number): object;

/**
* Greater than or equal to given number matching
*/
export function gte(value?: number): object;

/**
* Lesser than given number matching
*/
export function lt(value?: number): object;

/**
* Lesser than or equal to given number matching
*/
export function lte(value?: number): object;

/**
 * not have property in object
 */
export function notIncludes(value: string | number | boolean): object;
export function notIncludes(values: string[] | number[] | boolean[]): object;

/**
 * not null
 */
export function notNull(value: string | number | boolean): object;

/**
 * not equals
 */
 export function notEquals(value: string | number | boolean): object;

 /**
  * iso date format
  */
 export function iso(value?: string): object;