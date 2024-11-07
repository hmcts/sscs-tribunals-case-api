export interface ColorExceptionOptions {
    /**
     * True to enable ANSI colors.
     */
    color?: boolean;
    /**
     * basePath is used to determine whether a file is part of this project
     * or not.  If it is not set, then all files are considered part of the
     * project.
     */
    basePath?: string;
    basePathReplacement?: string;
    /**
     * Maximum number of lines of stack trace to show. undefined for unlimited.
     * "auto" truncate after "own" code.
     */
    maxLines?: number | 'auto';
}
/**
 * Format an exception for console output.
 */
export declare function formatException(err: Error | string, options: ColorExceptionOptions): string;
