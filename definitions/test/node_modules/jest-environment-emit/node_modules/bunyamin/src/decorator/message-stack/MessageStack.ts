/* eslint-disable @typescript-eslint/no-non-null-assertion */
import type { ThreadID } from '../../types';

type Message = unknown[];

export type MessageStackOptions = {
  /**
   * A string or any other value to be used as the message when a thread
   * is popped without any message being pushed previously.
   * @default '<no begin message>'
   */
  noBeginMessage?: unknown;
};

export class MessageStack {
  readonly #simple = new Map<unknown, Message[]>();
  readonly #complex = new Map<unknown, Map<unknown, Message[]>>();
  readonly #noBeginMessage: Message;

  constructor(options: MessageStackOptions = {}) {
    this.#noBeginMessage = [options.noBeginMessage ?? '<no begin message>'];
  }

  push(tid: ThreadID | undefined, message: unknown[]): void {
    const stack = this.#ensureStack(tid);
    stack.push(message);
  }

  pop(tid: ThreadID | undefined): unknown[] {
    const stack = this.#ensureStack(tid);
    return stack.pop() ?? this.#noBeginMessage;
  }

  #ensureStack(tid: ThreadID | undefined): Message[] {
    if (!Array.isArray(tid)) {
      if (!this.#simple.has(tid)) {
        this.#simple.set(tid, []);
      }

      return this.#simple.get(tid)!;
    }

    const [alias, subtid] = tid;
    if (!this.#complex.has(alias)) {
      this.#complex.set(alias, new Map());
    }

    const submap = this.#complex.get(alias)!;
    if (!submap.has(subtid)) {
      submap.set(subtid, []);
    }

    return submap.get(subtid)!;
  }
}
