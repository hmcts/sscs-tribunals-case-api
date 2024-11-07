import type { ThreadAlias } from '../../types';
export type BunyaminLogRecordFields = {
    [customProperty: string]: unknown;
    pid?: number;
    tid?: number | ThreadAlias;
    cat?: string | string[];
    cname?: string;
    ph?: never;
    time?: string;
};
