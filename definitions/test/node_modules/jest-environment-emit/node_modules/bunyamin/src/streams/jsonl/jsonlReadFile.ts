import fs from 'node:fs';
import type { Readable } from 'node:stream';
import { Transform } from 'node:stream';
// eslint-disable-next-line import/extensions
import StreamArray from 'stream-json/streamers/StreamArray.js';

export function jsonlReadFile(filePath: string): Readable {
  return fs
    .createReadStream(filePath, { encoding: 'utf8' })
    .pipe(StreamArray.withParser())
    .pipe(new MapValues(filePath));
}

class MapValues extends Transform {
  constructor(protected readonly filePath: string) {
    super({ objectMode: true });
  }

  _transform(
    record: any,
    _encoding: string,
    callback: (error?: Error | null, data?: unknown) => void,
  ) {
    this.push({
      ...record,
      filePath: this.filePath,
    } as JSONLEntry);

    callback();
  }
}

export type JSONLEntry<T = unknown> = {
  filePath: string;
  key: number;
  value: T;
};
