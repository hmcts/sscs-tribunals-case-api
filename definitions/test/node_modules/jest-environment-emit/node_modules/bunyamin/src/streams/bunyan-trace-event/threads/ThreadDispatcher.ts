import { isUndefined } from '../../../utils';

const NIL = Symbol('NIL');

export class ThreadDispatcher {
  readonly #stacks: number[] = [];
  readonly #threads: unknown[] = [];
  readonly #countMax: number;

  constructor(
    public readonly name: string,
    public readonly strict: boolean,
    public readonly min: number,
    public readonly max: number,
  ) {
    this.#countMax = max - min + 1;
  }

  begin(id: unknown = NIL): number | Error {
    const tid = this.#findTID(id);
    if (tid === -1) {
      return this.#error();
    }

    this.#threads[tid] = id;
    this.#stacks[tid] = (this.#stacks[tid] || 0) + 1;

    return this.#transposeTID(tid);
  }

  resolve(id: unknown = NIL): number | Error {
    const tid = this.#findTID(id);
    if (tid === -1) {
      return this.#error();
    }

    return this.#transposeTID(tid);
  }

  end(id: unknown = NIL): number | Error {
    const tid = this.#findTID(id);
    if (tid === -1) {
      return this.#error();
    }

    if (this.#stacks[tid] && --this.#stacks[tid] === 0) {
      delete this.#threads[tid];
    }

    return this.#transposeTID(tid);
  }

  #findTID(id: unknown): number {
    let tid = this.#threads.indexOf(id);
    if (tid === -1) {
      // Try to find a recently released slot in the array:
      tid = this.#threads.findIndex(isUndefined);
    }

    if (tid === -1) {
      tid = this.#threads.length;
    }

    return tid < this.#countMax ? tid : -1;
  }

  #transposeTID(tid: number): number {
    return this.min + tid;
  }

  #error(): number | Error {
    const count = this.#countMax;
    const threads = count > 1 ? `threads` : `thread`;

    return this.strict
      ? new Error(`Exceeded limit of ${count} concurrent ${threads} in group "${this.name}"`)
      : this.max;
  }
}
