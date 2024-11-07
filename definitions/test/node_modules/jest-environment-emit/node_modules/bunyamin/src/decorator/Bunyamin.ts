import { deflateCategories, mergeCategories } from './categories';
import { isSelfDebug } from '../is-debug';
import type { ThreadGroupConfig } from '../streams';
import type { ThreadID } from '../types';
import { flow, isActionable, isError, isObject, isPromiseLike } from '../utils';
import type {
  BunyaminConfig,
  BunyaminLogMethod,
  BunyaminLogRecordFields as UserFields,
  BunyanLikeLogger,
  BunyanLogLevel,
} from './types';
import { MessageStack } from './message-stack';
import { StackTraceError } from './StackTraceError';

export class Bunyamin<Logger extends BunyanLikeLogger = BunyanLikeLogger> {
  public readonly fatal = this.#setupLogMethod('fatal');
  public readonly error = this.#setupLogMethod('error');
  public readonly warn = this.#setupLogMethod('warn');
  public readonly info = this.#setupLogMethod('info');
  public readonly debug = this.#setupLogMethod('debug');
  public readonly trace = this.#setupLogMethod('trace');

  readonly #fields: PredefinedFields | undefined;
  /**
   * All instances of {@link Bunyamin} must share the same object instance
   */
  readonly #shared: SharedBunyaminConfig<Logger>;

  constructor(config: BunyaminConfig<Logger>, fields?: never);
  constructor(shared: unknown, fields?: unknown) {
    if (fields === undefined) {
      const config = shared as BunyaminConfig<Logger>;

      this.#fields = undefined;
      this.#shared = {
        ...config,
        loggerPriority: 0,
        messageStack: new MessageStack({
          noBeginMessage: config.noBeginMessage,
        }),
      };
    } else {
      this.#fields = fields as PredefinedFields;
      this.#shared = shared as SharedBunyaminConfig<Logger>;
    }
  }

  /** @deprecated */
  get threadGroups(): ThreadGroupConfig[] {
    return [...(this.#shared.threadGroups ?? [])];
  }

  get logger(): Logger {
    return this.#shared.logger;
  }

  set logger(logger: Logger) {
    this.useLogger(logger);
  }

  useLogger(logger: Logger, priority = 0): this {
    this.#assertNotImmutable();
    this.#assertNotChild('useLogger');

    const { stack } = isSelfDebug() ? new StackTraceError() : StackTraceError.empty();
    const currentPriority = this.#shared.loggerPriority;
    if (priority >= currentPriority) {
      this.#shared.loggerPriority = priority;
      this.#shared.logger = logger;
      stack &&
        this.#shared.logger.trace(
          { cat: 'bunyamin' },
          `bunyamin logger changed (${priority} >= ${currentPriority}), caller was:\n${stack}`,
        );
    } else {
      stack &&
        this.#shared.logger.trace(
          { cat: 'bunyamin' },
          `bunyamin logger not changed (${priority} < ${currentPriority}), caller was:\n${stack}`,
        );
    }

    return this;
  }

  useTransform(transformFields: Required<BunyaminConfig<Logger>>['transformFields']): this {
    this.#assertNotImmutable();
    this.#assertNotChild('useTransform');

    this.#shared.transformFields = this.#shared.transformFields
      ? flow(this.#shared.transformFields, transformFields)
      : transformFields;

    return this;
  }

  child(overrides?: UserFields): Bunyamin<Logger> {
    const childContext = this.#mergeFields(this.#fields, this.#transformContext(overrides));
    return new Bunyamin(this.#shared, childContext as never);
  }

  #setupLogMethod(level: BunyanLogLevel): BunyaminLogMethod {
    const logMethod = this.#instant.bind(this, level);

    return Object.assign(logMethod, {
      begin: this.#begin.bind(this, level),
      complete: this.#complete.bind(this, level),
      end: this.#end.bind(this, level),
    }) as BunyaminLogMethod;
  }

  #begin(level: BunyanLogLevel, ...arguments_: unknown[]): void {
    const entry = this.#resolveLogEntry('B', arguments_);
    this.#beginInternal(level, entry.fields, entry.message);
  }

  #beginInternal(level: BunyanLogLevel, fields: ResolvedFields, message: unknown[]): void {
    this.#shared.messageStack.push(fields.tid, message);
    this.#shared.logger[level](fields, ...message);
  }

  #end(level: BunyanLogLevel, ...arguments_: unknown[]): void {
    const entry = this.#resolveLogEntry('E', arguments_);
    this.#endInternal(level, entry.fields, entry.message);
  }

  #endInternal(level: BunyanLogLevel, fields: ResolvedFields, customMessage: unknown[]): void {
    const beginMessage = this.#shared.messageStack.pop(fields.tid);
    const message = customMessage.length > 0 ? customMessage : beginMessage;

    this.#shared.logger[level](fields, ...(message as unknown[]));
  }

  #instant(level: BunyanLogLevel, ...arguments_: unknown[]): void {
    const entry = this.#resolveLogEntry(void 0, arguments_);
    this.#shared.logger[level](entry.fields, ...entry.message);
  }

  #complete<T>(
    level: BunyanLogLevel,
    maybeContext: unknown,
    maybeMessage: unknown,
    maybeAction: T | (() => T),
  ): T {
    const action = typeof maybeContext === 'string' ? (maybeMessage as T | (() => T)) : maybeAction;
    const arguments_ = maybeAction === action ? [maybeContext, maybeMessage] : [maybeContext];
    const { fields, message } = this.#resolveLogEntry('B', arguments_);

    return this.#completeInternal(level, fields, message, action);
  }

  #completeInternal<T>(
    level: BunyanLogLevel,
    fields: ResolvedFields,
    message: unknown[],
    action: T | (() => T),
  ): T {
    const end = (customContext: EndContext) => {
      const endContext = {
        ...this.#transformContext(customContext),
        ph: 'E',
      } as ResolvedFields;

      if (fields.tid !== undefined) {
        endContext.tid = fields.tid;
      }

      if (fields.level !== undefined) {
        endContext.level = fields.level;
      }

      this.#endInternal(level, endContext, []);
    };

    let result;
    this.#beginInternal(level, fields, message);
    try {
      result = isActionable(action) ? action() : action;

      if (isPromiseLike(result)) {
        result.then(
          () => end({ success: true }),
          (error) => end({ success: false, err: error }),
        );
      } else {
        end({ success: true });
      }

      return result;
    } catch (error: unknown) {
      end({ success: false, err: error });
      throw error;
    }
  }

  #resolveLogEntry(phase: MaybePhase, arguments_: unknown[]) {
    const userContext = isObject(arguments_[0]) ? (arguments_[0] as MaybeUserFields) : undefined;
    const fields = this.#mergeFields(this.#fields, this.#transformContext(userContext));
    const message: unknown[] =
      userContext === undefined
        ? arguments_
        : isError(arguments_[0]) && arguments_.length === 1
          ? [arguments_[0].message]
          : arguments_.slice(1);

    return {
      fields: this.#resolveFields(fields, phase),
      message,
    };
  }

  #mergeFields(
    left: PredefinedFields | undefined,
    right: UserFields | undefined,
  ): PredefinedFields {
    const result = {
      ...left,
      ...right,
    };

    const cat = mergeCategories(left?.cat, right?.cat);
    if (result.cat !== cat) {
      result.cat = cat;
    }

    return result as PredefinedFields;
  }

  #transformContext(maybeError: UserFields | Error | undefined): UserFields | undefined {
    const fields: UserFields | undefined = isError(maybeError) ? { err: maybeError } : maybeError;
    return this.#shared.transformFields ? this.#shared.transformFields(fields) : fields;
  }

  #resolveFields(fields: PredefinedFields, ph: MaybePhase): ResolvedFields {
    const result: ResolvedFields = fields as ResolvedFields;
    if (ph !== undefined) {
      result.ph = ph as never;
    }
    if (result.cat !== undefined) {
      result.cat = deflateCategories(result.cat);
    }
    return result;
  }

  #assertNotChild(methodName: string): void {
    if (this.#fields) {
      throw new Error(`Method Bunyamin#${methodName} is not available for child instances`);
    }
  }

  #assertNotImmutable(): void {
    if (this.#shared.immutable) {
      throw new Error('Cannot change a logger of an immutable instance');
    }
  }
}

type EndContext = {
  success?: boolean;
  err?: unknown;
};

type MaybePhase = 'B' | 'E' | undefined;

type MaybeUserFields = UserFields | Error;

type PredefinedFields = UserFields & {
  cat?: string[];
};

type ResolvedFields = UserFields & {
  cat?: string;
  ph?: 'B' | 'E';
  tid?: ThreadID;
  /**
   * 10 - trace
   * 20 - debug
   * 30 - info
   * 40 - warn
   * 50 - error
   * 60 - fatal
   */
  level?: number;
};

type SharedBunyaminConfig<Logger extends BunyanLikeLogger> = BunyaminConfig<Logger> & {
  loggerPriority: number;
  messageStack: MessageStack;
};
