import type { Transform } from 'node:stream';
import { BunyanTraceEventStream, jsonlWriteFile } from './streams';
import type { TraceEventStreamOptions } from './streams';

export function traceEventStream(
  options: TraceEventStreamOptions & { filePath: string },
): Transform {
  const jsonl = jsonlWriteFile(options.filePath);
  const stream = new BunyanTraceEventStream(options);
  stream.pipe(jsonl);
  return stream;
}
