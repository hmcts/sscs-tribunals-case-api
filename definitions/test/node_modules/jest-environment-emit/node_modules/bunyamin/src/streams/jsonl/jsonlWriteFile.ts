import fs from 'node:fs';
import { Writable } from 'node:stream';

export function jsonlWriteFile(filePath: string): Writable {
  return new JSONLFileStream({ filePath });
}

type JSONLFileStreamOptions = {
  filePath: string;
};

type ErrorCallback = (error?: Error | null) => void;

// Custom writable stream to append JSON objects
class JSONLFileStream extends Writable {
  private readonly _filePath: string;
  private _fileDescriptor = Number.NaN;
  private _offset = Number.NaN;
  private _counter = 0;

  constructor(options: JSONLFileStreamOptions) {
    super({ objectMode: true });

    this._filePath = options.filePath;
  }

  _construct(callback: ErrorCallback) {
    this._offset = 0;
    this._fileDescriptor = fs.openSync(this._filePath, 'wx');

    const content = Buffer.from('[]\n');
    fs.write(this._fileDescriptor, content, this._offset, content.length, (error) => {
      if (error) {
        callback(error);
      } else {
        this._offset += 1;
        callback();
      }
    });
  }

  _write(chunk: unknown, _: unknown, callback: ErrorCallback) {
    const content =
      this._counter++ > 0 ? `,\n${JSON.stringify(chunk)}]\n` : `${JSON.stringify(chunk)}]\n`;
    const buffer = Buffer.from(content);

    fs.write(
      this._fileDescriptor,
      buffer,
      0,
      buffer.length,
      this._offset,
      (error: Error | null, bytesWritten: number) => {
        if (error) {
          callback(error);
        } else {
          this._offset += bytesWritten - 2;
          callback();
        }
      },
    );
  }

  _final(callback: ErrorCallback) {
    fs.close(this._fileDescriptor, callback);
  }
}
