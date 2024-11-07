import type { Readable } from 'node:stream';
import type { TraceMergeOptions } from './streams';
import { jsonlWriteFile, traceMerge } from './streams';

export function uniteTraceEvents(sourcePaths: string[], options?: TraceMergeOptions): Readable {
  return traceMerge(sourcePaths, options);
}

export async function uniteTraceEventsToFile(
  sourcePaths: string[],
  destinationPath: string,
  options?: TraceMergeOptions,
) {
  return new Promise((resolve, reject) => {
    uniteTraceEvents(sourcePaths, options)
      .pipe(jsonlWriteFile(destinationPath))
      .on('finish', resolve)
      .on('error', reject);
  });
}
