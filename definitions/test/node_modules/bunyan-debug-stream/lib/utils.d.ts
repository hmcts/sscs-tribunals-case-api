export declare function lpad(str: string | number, count: number, fill?: string): string;
export declare function dateToString(date: Date | undefined): string | undefined;
export declare function applyColors(message: string, colorList: string[]): string;
export declare function toShortFilename(filename: string, basepath?: string | undefined, replacement?: string): string;
export declare function srcToString(src: {
    file: string;
    line: number;
    func: string;
}, basepath?: string | undefined, replacement?: string): string;
