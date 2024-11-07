(function (global, factory) {
  typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('node:stream'), require('@flatten-js/interval-tree'), require('trace-event-lib'), require('node:fs'), require('stream-json/streamers/StreamArray.js'), require('multi-sort-stream')) :
  typeof define === 'function' && define.amd ? define(['exports', 'node:stream', '@flatten-js/interval-tree', 'trace-event-lib', 'node:fs', 'stream-json/streamers/StreamArray.js', 'multi-sort-stream'], factory) :
  (global = global || self, factory(global.bunyamin = {}, global.node_stream, global.IntervalTree, global.traceEventLib, global.fs, global.StreamArray, global.multiSortStream));
})(this, (function (exports, node_stream, IntervalTree, TEL, fs, StreamArray, multiSortStream) {
  function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e : { 'default': e }; }

  function _interopNamespace(e) {
    if (e && e.__esModule) return e;
    var n = Object.create(null);
    if (e) {
      Object.keys(e).forEach(function (k) {
        if (k !== 'default') {
          var d = Object.getOwnPropertyDescriptor(e, k);
          Object.defineProperty(n, k, d.get ? d : {
            enumerable: true,
            get: function () { return e[k]; }
          });
        }
      });
    }
    n["default"] = e;
    return n;
  }

  var IntervalTree__default = /*#__PURE__*/_interopDefaultLegacy(IntervalTree);
  var TEL__namespace = /*#__PURE__*/_interopNamespace(TEL);
  var fs__default = /*#__PURE__*/_interopDefaultLegacy(fs);
  var StreamArray__default = /*#__PURE__*/_interopDefaultLegacy(StreamArray);
  var multiSortStream__default = /*#__PURE__*/_interopDefaultLegacy(multiSortStream);

  function _classPrivateFieldLooseBase(e, t) {
    if (!{}.hasOwnProperty.call(e, t)) throw new TypeError("attempted to use private field on non-instance");
    return e;
  }
  var id = 0;
  function _classPrivateFieldLooseKey(e) {
    return "__private_" + id++ + "_" + e;
  }

  function deflateCategories(cat) {
    if (!cat) {
      return undefined;
    }
    if (Array.isArray(cat)) {
      return cat.filter(Boolean).join(',');
    }
    return String(cat);
  }

  function inflateCategories(cat) {
    if (!cat) {
      return [];
    }
    if (Array.isArray(cat)) {
      return cat;
    }
    return String(cat).split(',');
  }

  function mergeCategories(left, right) {
    if (!left || !right) {
      if (left) return left;
      if (right) return inflateCategories(right);
      return undefined;
    }
    const iright = inflateCategories(right);
    const categories = left ? [...left, ...iright] : iright;
    const uniqueCategories = new Set(categories);
    return [...uniqueCategories.values()];
  }

  function createIsDebug(namespaces) {
    const skips = [];
    const names = [];
    for (const part of namespaces.split(/[\s,]+/)) {
      if (!part) {
        continue;
      }
      const destination = part[0] === '-' ? skips : names;
      const pattern = part.replace(/^-/, '').replace(/\*/g, '.*?');
      destination.push(new RegExp(`^${pattern}$`));
    }
    return function isDebug(name) {
      if (name[name.length - 1] === '*') {
        return true;
      }
      if (skips.some(regex => regex.test(name))) {
        return false;
      }
      if (names.some(regex => regex.test(name))) {
        return true;
      }
      return false;
    };
  }

  const isDebug = createIsDebug(process.env.DEBUG || '');
  const isSelfDebug = () => isDebug('bunyamin');

  function flow(f, g) {
    return x => g(f(x));
  }

  function isActionable(value) {
    return typeof value === 'function';
  }

  function isError(value) {
    return value instanceof Error;
  }

  function isPromiseLike(maybePromise) {
    return maybePromise ? typeof maybePromise.then === 'function' : false;
  }

  function isObject(value) {
    return value ? typeof value === 'object' : false;
  }

  function isUndefined(x) {
    return x === undefined;
  }

  var _simple = /*#__PURE__*/_classPrivateFieldLooseKey("simple");
  var _complex = /*#__PURE__*/_classPrivateFieldLooseKey("complex");
  var _noBeginMessage = /*#__PURE__*/_classPrivateFieldLooseKey("noBeginMessage");
  var _ensureStack = /*#__PURE__*/_classPrivateFieldLooseKey("ensureStack");
  class MessageStack {
    constructor(options = {}) {
      var _options$noBeginMessa;
      Object.defineProperty(this, _ensureStack, {
        value: _ensureStack2
      });
      Object.defineProperty(this, _simple, {
        writable: true,
        value: new Map()
      });
      Object.defineProperty(this, _complex, {
        writable: true,
        value: new Map()
      });
      Object.defineProperty(this, _noBeginMessage, {
        writable: true,
        value: void 0
      });
      _classPrivateFieldLooseBase(this, _noBeginMessage)[_noBeginMessage] = [(_options$noBeginMessa = options.noBeginMessage) != null ? _options$noBeginMessa : '<no begin message>'];
    }
    push(tid, message) {
      const stack = _classPrivateFieldLooseBase(this, _ensureStack)[_ensureStack](tid);
      stack.push(message);
    }
    pop(tid) {
      var _stack$pop;
      const stack = _classPrivateFieldLooseBase(this, _ensureStack)[_ensureStack](tid);
      return (_stack$pop = stack.pop()) != null ? _stack$pop : _classPrivateFieldLooseBase(this, _noBeginMessage)[_noBeginMessage];
    }
  }
  function _ensureStack2(tid) {
    if (!Array.isArray(tid)) {
      if (!_classPrivateFieldLooseBase(this, _simple)[_simple].has(tid)) {
        _classPrivateFieldLooseBase(this, _simple)[_simple].set(tid, []);
      }
      return _classPrivateFieldLooseBase(this, _simple)[_simple].get(tid);
    }
    const [alias, subtid] = tid;
    if (!_classPrivateFieldLooseBase(this, _complex)[_complex].has(alias)) {
      _classPrivateFieldLooseBase(this, _complex)[_complex].set(alias, new Map());
    }
    const submap = _classPrivateFieldLooseBase(this, _complex)[_complex].get(alias);
    if (!submap.has(subtid)) {
      submap.set(subtid, []);
    }
    return submap.get(subtid);
  }

  class StackTraceError extends Error {
    constructor() {
      super('Providing stack trace below:');
      this.name = 'StackTrace';
    }
    static empty() {
      return {
        message: '',
        stack: ''
      };
    }
  }

  var _fields = /*#__PURE__*/_classPrivateFieldLooseKey("fields");
  var _shared = /*#__PURE__*/_classPrivateFieldLooseKey("shared");
  var _setupLogMethod = /*#__PURE__*/_classPrivateFieldLooseKey("setupLogMethod");
  var _begin = /*#__PURE__*/_classPrivateFieldLooseKey("begin");
  var _beginInternal = /*#__PURE__*/_classPrivateFieldLooseKey("beginInternal");
  var _end = /*#__PURE__*/_classPrivateFieldLooseKey("end");
  var _endInternal = /*#__PURE__*/_classPrivateFieldLooseKey("endInternal");
  var _instant = /*#__PURE__*/_classPrivateFieldLooseKey("instant");
  var _complete = /*#__PURE__*/_classPrivateFieldLooseKey("complete");
  var _completeInternal = /*#__PURE__*/_classPrivateFieldLooseKey("completeInternal");
  var _resolveLogEntry = /*#__PURE__*/_classPrivateFieldLooseKey("resolveLogEntry");
  var _mergeFields = /*#__PURE__*/_classPrivateFieldLooseKey("mergeFields");
  var _transformContext = /*#__PURE__*/_classPrivateFieldLooseKey("transformContext");
  var _resolveFields = /*#__PURE__*/_classPrivateFieldLooseKey("resolveFields");
  var _assertNotChild = /*#__PURE__*/_classPrivateFieldLooseKey("assertNotChild");
  var _assertNotImmutable = /*#__PURE__*/_classPrivateFieldLooseKey("assertNotImmutable");
  class Bunyamin {
    constructor(shared, _fields2) {
      Object.defineProperty(this, _assertNotImmutable, {
        value: _assertNotImmutable2
      });
      Object.defineProperty(this, _assertNotChild, {
        value: _assertNotChild2
      });
      Object.defineProperty(this, _resolveFields, {
        value: _resolveFields2
      });
      Object.defineProperty(this, _transformContext, {
        value: _transformContext2
      });
      Object.defineProperty(this, _mergeFields, {
        value: _mergeFields2
      });
      Object.defineProperty(this, _resolveLogEntry, {
        value: _resolveLogEntry2
      });
      Object.defineProperty(this, _completeInternal, {
        value: _completeInternal2
      });
      Object.defineProperty(this, _complete, {
        value: _complete2
      });
      Object.defineProperty(this, _instant, {
        value: _instant2
      });
      Object.defineProperty(this, _endInternal, {
        value: _endInternal2
      });
      Object.defineProperty(this, _end, {
        value: _end2
      });
      Object.defineProperty(this, _beginInternal, {
        value: _beginInternal2
      });
      Object.defineProperty(this, _begin, {
        value: _begin2
      });
      Object.defineProperty(this, _setupLogMethod, {
        value: _setupLogMethod2
      });
      this.fatal = _classPrivateFieldLooseBase(this, _setupLogMethod)[_setupLogMethod]('fatal');
      this.error = _classPrivateFieldLooseBase(this, _setupLogMethod)[_setupLogMethod]('error');
      this.warn = _classPrivateFieldLooseBase(this, _setupLogMethod)[_setupLogMethod]('warn');
      this.info = _classPrivateFieldLooseBase(this, _setupLogMethod)[_setupLogMethod]('info');
      this.debug = _classPrivateFieldLooseBase(this, _setupLogMethod)[_setupLogMethod]('debug');
      this.trace = _classPrivateFieldLooseBase(this, _setupLogMethod)[_setupLogMethod]('trace');
      Object.defineProperty(this, _fields, {
        writable: true,
        value: void 0
      });
      Object.defineProperty(this, _shared, {
        writable: true,
        value: void 0
      });
      if (_fields2 === undefined) {
        const config = shared;
        _classPrivateFieldLooseBase(this, _fields)[_fields] = undefined;
        _classPrivateFieldLooseBase(this, _shared)[_shared] = {
          ...config,
          loggerPriority: 0,
          messageStack: new MessageStack({
            noBeginMessage: config.noBeginMessage
          })
        };
      } else {
        _classPrivateFieldLooseBase(this, _fields)[_fields] = _fields2;
        _classPrivateFieldLooseBase(this, _shared)[_shared] = shared;
      }
    }
    get threadGroups() {
      var _classPrivateFieldLoo;
      return [...((_classPrivateFieldLoo = _classPrivateFieldLooseBase(this, _shared)[_shared].threadGroups) != null ? _classPrivateFieldLoo : [])];
    }
    get logger() {
      return _classPrivateFieldLooseBase(this, _shared)[_shared].logger;
    }
    set logger(logger) {
      this.useLogger(logger);
    }
    useLogger(logger, priority = 0) {
      _classPrivateFieldLooseBase(this, _assertNotImmutable)[_assertNotImmutable]();
      _classPrivateFieldLooseBase(this, _assertNotChild)[_assertNotChild]('useLogger');
      const {
        stack
      } = isSelfDebug() ? new StackTraceError() : StackTraceError.empty();
      const currentPriority = _classPrivateFieldLooseBase(this, _shared)[_shared].loggerPriority;
      if (priority >= currentPriority) {
        _classPrivateFieldLooseBase(this, _shared)[_shared].loggerPriority = priority;
        _classPrivateFieldLooseBase(this, _shared)[_shared].logger = logger;
        stack && _classPrivateFieldLooseBase(this, _shared)[_shared].logger.trace({
          cat: 'bunyamin'
        }, `bunyamin logger changed (${priority} >= ${currentPriority}), caller was:\n${stack}`);
      } else {
        stack && _classPrivateFieldLooseBase(this, _shared)[_shared].logger.trace({
          cat: 'bunyamin'
        }, `bunyamin logger not changed (${priority} < ${currentPriority}), caller was:\n${stack}`);
      }
      return this;
    }
    useTransform(transformFields) {
      _classPrivateFieldLooseBase(this, _assertNotImmutable)[_assertNotImmutable]();
      _classPrivateFieldLooseBase(this, _assertNotChild)[_assertNotChild]('useTransform');
      _classPrivateFieldLooseBase(this, _shared)[_shared].transformFields = _classPrivateFieldLooseBase(this, _shared)[_shared].transformFields ? flow(_classPrivateFieldLooseBase(this, _shared)[_shared].transformFields, transformFields) : transformFields;
      return this;
    }
    child(overrides) {
      const childContext = _classPrivateFieldLooseBase(this, _mergeFields)[_mergeFields](_classPrivateFieldLooseBase(this, _fields)[_fields], _classPrivateFieldLooseBase(this, _transformContext)[_transformContext](overrides));
      return new Bunyamin(_classPrivateFieldLooseBase(this, _shared)[_shared], childContext);
    }
  }
  function _setupLogMethod2(level) {
    const logMethod = _classPrivateFieldLooseBase(this, _instant)[_instant].bind(this, level);
    return Object.assign(logMethod, {
      begin: _classPrivateFieldLooseBase(this, _begin)[_begin].bind(this, level),
      complete: _classPrivateFieldLooseBase(this, _complete)[_complete].bind(this, level),
      end: _classPrivateFieldLooseBase(this, _end)[_end].bind(this, level)
    });
  }
  function _begin2(level, ...arguments_) {
    const entry = _classPrivateFieldLooseBase(this, _resolveLogEntry)[_resolveLogEntry]('B', arguments_);
    _classPrivateFieldLooseBase(this, _beginInternal)[_beginInternal](level, entry.fields, entry.message);
  }
  function _beginInternal2(level, fields, message) {
    _classPrivateFieldLooseBase(this, _shared)[_shared].messageStack.push(fields.tid, message);
    _classPrivateFieldLooseBase(this, _shared)[_shared].logger[level](fields, ...message);
  }
  function _end2(level, ...arguments_) {
    const entry = _classPrivateFieldLooseBase(this, _resolveLogEntry)[_resolveLogEntry]('E', arguments_);
    _classPrivateFieldLooseBase(this, _endInternal)[_endInternal](level, entry.fields, entry.message);
  }
  function _endInternal2(level, fields, customMessage) {
    const beginMessage = _classPrivateFieldLooseBase(this, _shared)[_shared].messageStack.pop(fields.tid);
    const message = customMessage.length > 0 ? customMessage : beginMessage;
    _classPrivateFieldLooseBase(this, _shared)[_shared].logger[level](fields, ...message);
  }
  function _instant2(level, ...arguments_) {
    const entry = _classPrivateFieldLooseBase(this, _resolveLogEntry)[_resolveLogEntry](void 0, arguments_);
    _classPrivateFieldLooseBase(this, _shared)[_shared].logger[level](entry.fields, ...entry.message);
  }
  function _complete2(level, maybeContext, maybeMessage, maybeAction) {
    const action = typeof maybeContext === 'string' ? maybeMessage : maybeAction;
    const arguments_ = maybeAction === action ? [maybeContext, maybeMessage] : [maybeContext];
    const {
      fields,
      message
    } = _classPrivateFieldLooseBase(this, _resolveLogEntry)[_resolveLogEntry]('B', arguments_);
    return _classPrivateFieldLooseBase(this, _completeInternal)[_completeInternal](level, fields, message, action);
  }
  function _completeInternal2(level, fields, message, action) {
    const end = customContext => {
      const endContext = {
        ..._classPrivateFieldLooseBase(this, _transformContext)[_transformContext](customContext),
        ph: 'E'
      };
      if (fields.tid !== undefined) {
        endContext.tid = fields.tid;
      }
      if (fields.level !== undefined) {
        endContext.level = fields.level;
      }
      _classPrivateFieldLooseBase(this, _endInternal)[_endInternal](level, endContext, []);
    };
    let result;
    _classPrivateFieldLooseBase(this, _beginInternal)[_beginInternal](level, fields, message);
    try {
      result = isActionable(action) ? action() : action;
      if (isPromiseLike(result)) {
        result.then(() => end({
          success: true
        }), error => end({
          success: false,
          err: error
        }));
      } else {
        end({
          success: true
        });
      }
      return result;
    } catch (error) {
      end({
        success: false,
        err: error
      });
      throw error;
    }
  }
  function _resolveLogEntry2(phase, arguments_) {
    const userContext = isObject(arguments_[0]) ? arguments_[0] : undefined;
    const fields = _classPrivateFieldLooseBase(this, _mergeFields)[_mergeFields](_classPrivateFieldLooseBase(this, _fields)[_fields], _classPrivateFieldLooseBase(this, _transformContext)[_transformContext](userContext));
    const message = userContext === undefined ? arguments_ : isError(arguments_[0]) && arguments_.length === 1 ? [arguments_[0].message] : arguments_.slice(1);
    return {
      fields: _classPrivateFieldLooseBase(this, _resolveFields)[_resolveFields](fields, phase),
      message
    };
  }
  function _mergeFields2(left, right) {
    const result = {
      ...left,
      ...right
    };
    const cat = mergeCategories(left == null ? void 0 : left.cat, right == null ? void 0 : right.cat);
    if (result.cat !== cat) {
      result.cat = cat;
    }
    return result;
  }
  function _transformContext2(maybeError) {
    const fields = isError(maybeError) ? {
      err: maybeError
    } : maybeError;
    return _classPrivateFieldLooseBase(this, _shared)[_shared].transformFields ? _classPrivateFieldLooseBase(this, _shared)[_shared].transformFields(fields) : fields;
  }
  function _resolveFields2(fields, ph) {
    const result = fields;
    if (ph !== undefined) {
      result.ph = ph;
    }
    if (result.cat !== undefined) {
      result.cat = deflateCategories(result.cat);
    }
    return result;
  }
  function _assertNotChild2(methodName) {
    if (_classPrivateFieldLooseBase(this, _fields)[_fields]) {
      throw new Error(`Method Bunyamin#${methodName} is not available for child instances`);
    }
  }
  function _assertNotImmutable2() {
    if (_classPrivateFieldLooseBase(this, _shared)[_shared].immutable) {
      throw new Error('Cannot change a logger of an immutable instance');
    }
  }

  const noop = () => {};
  class NoopLogger {
    constructor() {
      this.fatal = noop;
      this.error = noop;
      this.warn = noop;
      this.info = noop;
      this.debug = noop;
      this.trace = noop;
    }
  }
  function noopLogger(_options) {
    return new NoopLogger();
  }

  var _debugMode = /*#__PURE__*/_classPrivateFieldLooseKey("debugMode");
  var _getBunyamin = /*#__PURE__*/_classPrivateFieldLooseKey("getBunyamin");
  var _groups = /*#__PURE__*/_classPrivateFieldLooseKey("groups");
  var _logAddition = /*#__PURE__*/_classPrivateFieldLooseKey("logAddition");
  class ThreadGroups {
    constructor(getBunyamin) {
      Object.defineProperty(this, _logAddition, {
        value: _logAddition2
      });
      Object.defineProperty(this, _debugMode, {
        writable: true,
        value: isSelfDebug()
      });
      Object.defineProperty(this, _getBunyamin, {
        writable: true,
        value: void 0
      });
      Object.defineProperty(this, _groups, {
        writable: true,
        value: new Map()
      });
      _classPrivateFieldLooseBase(this, _getBunyamin)[_getBunyamin] = getBunyamin;
      _classPrivateFieldLooseBase(this, _groups)[_groups] = new Map();
    }
    add(group) {
      if (_classPrivateFieldLooseBase(this, _debugMode)[_debugMode]) {
        if (_classPrivateFieldLooseBase(this, _groups)[_groups].has(group.id)) {
          _classPrivateFieldLooseBase(this, _logAddition)[_logAddition](group, 'overwritten');
        } else {
          _classPrivateFieldLooseBase(this, _logAddition)[_logAddition](group, 'added');
        }
      }
      _classPrivateFieldLooseBase(this, _groups)[_groups].set(group.id, group);
      return this;
    }
    [Symbol.iterator]() {
      return _classPrivateFieldLooseBase(this, _groups)[_groups].values();
    }
  }
  function _logAddition2(group, action) {
    const {
      stack
    } = new StackTraceError();
    _classPrivateFieldLooseBase(this, _getBunyamin)[_getBunyamin]().trace({
      cat: 'bunyamin'
    }, `thread group ${action}: ${group.id} (${group.displayName})\n\n${stack}`);
  }

  var _getCached;
  function create() {
    let bunyamin;
    let nobunyamin;
    const selfDebug = isSelfDebug();
    const threadGroups = new ThreadGroups(() => bunyamin);
    bunyamin = new Bunyamin({
      logger: noopLogger(),
      threadGroups
    });
    nobunyamin = new Bunyamin({
      immutable: true,
      logger: noopLogger(),
      threadGroups
    });
    if (selfDebug) {
      bunyamin.trace({
        cat: 'bunyamin'
      }, 'bunyamin global instance created');
    }
    return {
      bunyamin,
      nobunyamin,
      threadGroups
    };
  }
  function getCached() {
    const result = globalThis.__BUNYAMIN__;
    if (isSelfDebug() && result) {
      result.bunyamin.trace({
        cat: 'bunyamin'
      }, 'bunyamin global instance retrieved from cache');
    }
    return result;
  }
  function setCached(realm) {
    globalThis.__BUNYAMIN__ = realm;
    return realm;
  }
  var realm = setCached((_getCached = getCached()) != null ? _getCached : create());

  const NIL = Symbol('NIL');
  var _stacks = /*#__PURE__*/_classPrivateFieldLooseKey("stacks");
  var _threads = /*#__PURE__*/_classPrivateFieldLooseKey("threads");
  var _countMax = /*#__PURE__*/_classPrivateFieldLooseKey("countMax");
  var _findTID = /*#__PURE__*/_classPrivateFieldLooseKey("findTID");
  var _transposeTID = /*#__PURE__*/_classPrivateFieldLooseKey("transposeTID");
  var _error = /*#__PURE__*/_classPrivateFieldLooseKey("error");
  class ThreadDispatcher {
    constructor(name, strict, min, max) {
      Object.defineProperty(this, _error, {
        value: _error2
      });
      Object.defineProperty(this, _transposeTID, {
        value: _transposeTID2
      });
      Object.defineProperty(this, _findTID, {
        value: _findTID2
      });
      this.name = void 0;
      this.strict = void 0;
      this.min = void 0;
      this.max = void 0;
      Object.defineProperty(this, _stacks, {
        writable: true,
        value: []
      });
      Object.defineProperty(this, _threads, {
        writable: true,
        value: []
      });
      Object.defineProperty(this, _countMax, {
        writable: true,
        value: void 0
      });
      this.name = name;
      this.strict = strict;
      this.min = min;
      this.max = max;
      _classPrivateFieldLooseBase(this, _countMax)[_countMax] = max - min + 1;
    }
    begin(id = NIL) {
      const tid = _classPrivateFieldLooseBase(this, _findTID)[_findTID](id);
      if (tid === -1) {
        return _classPrivateFieldLooseBase(this, _error)[_error]();
      }
      _classPrivateFieldLooseBase(this, _threads)[_threads][tid] = id;
      _classPrivateFieldLooseBase(this, _stacks)[_stacks][tid] = (_classPrivateFieldLooseBase(this, _stacks)[_stacks][tid] || 0) + 1;
      return _classPrivateFieldLooseBase(this, _transposeTID)[_transposeTID](tid);
    }
    resolve(id = NIL) {
      const tid = _classPrivateFieldLooseBase(this, _findTID)[_findTID](id);
      if (tid === -1) {
        return _classPrivateFieldLooseBase(this, _error)[_error]();
      }
      return _classPrivateFieldLooseBase(this, _transposeTID)[_transposeTID](tid);
    }
    end(id = NIL) {
      const tid = _classPrivateFieldLooseBase(this, _findTID)[_findTID](id);
      if (tid === -1) {
        return _classPrivateFieldLooseBase(this, _error)[_error]();
      }
      if (_classPrivateFieldLooseBase(this, _stacks)[_stacks][tid] && --_classPrivateFieldLooseBase(this, _stacks)[_stacks][tid] === 0) {
        delete _classPrivateFieldLooseBase(this, _threads)[_threads][tid];
      }
      return _classPrivateFieldLooseBase(this, _transposeTID)[_transposeTID](tid);
    }
  }
  function _findTID2(id) {
    let tid = _classPrivateFieldLooseBase(this, _threads)[_threads].indexOf(id);
    if (tid === -1) {
      tid = _classPrivateFieldLooseBase(this, _threads)[_threads].findIndex(isUndefined);
    }
    if (tid === -1) {
      tid = _classPrivateFieldLooseBase(this, _threads)[_threads].length;
    }
    return tid < _classPrivateFieldLooseBase(this, _countMax)[_countMax] ? tid : -1;
  }
  function _transposeTID2(tid) {
    return this.min + tid;
  }
  function _error2() {
    const count = _classPrivateFieldLooseBase(this, _countMax)[_countMax];
    const threads = count > 1 ? `threads` : `thread`;
    return this.strict ? new Error(`Exceeded limit of ${count} concurrent ${threads} in group "${this.name}"`) : this.max;
  }

  var _strict = /*#__PURE__*/_classPrivateFieldLooseKey("strict");
  var _dispatchers = /*#__PURE__*/_classPrivateFieldLooseKey("dispatchers");
  var _maxConcurrency = /*#__PURE__*/_classPrivateFieldLooseKey("maxConcurrency");
  var _defaultThreadName = /*#__PURE__*/_classPrivateFieldLooseKey("defaultThreadName");
  var _threadGroups = /*#__PURE__*/_classPrivateFieldLooseKey("threadGroups");
  var _names = /*#__PURE__*/_classPrivateFieldLooseKey("names");
  var _freeThreadId = /*#__PURE__*/_classPrivateFieldLooseKey("freeThreadId");
  var _initialized = /*#__PURE__*/_classPrivateFieldLooseKey("initialized");
  var _ensureInitialized = /*#__PURE__*/_classPrivateFieldLooseKey("ensureInitialized");
  var _registerThreadGroup = /*#__PURE__*/_classPrivateFieldLooseKey("registerThreadGroup");
  var _resolveDispatcher = /*#__PURE__*/_classPrivateFieldLooseKey("resolveDispatcher");
  var _resolveAlias = /*#__PURE__*/_classPrivateFieldLooseKey("resolveAlias");
  var _resolveId = /*#__PURE__*/_classPrivateFieldLooseKey("resolveId");
  var _ensureGroupDispatcher = /*#__PURE__*/_classPrivateFieldLooseKey("ensureGroupDispatcher");
  class ThreadGroupDispatcher {
    constructor(options) {
      Object.defineProperty(this, _ensureGroupDispatcher, {
        value: _ensureGroupDispatcher2
      });
      Object.defineProperty(this, _resolveId, {
        value: _resolveId2
      });
      Object.defineProperty(this, _resolveAlias, {
        value: _resolveAlias2
      });
      Object.defineProperty(this, _resolveDispatcher, {
        value: _resolveDispatcher2
      });
      Object.defineProperty(this, _registerThreadGroup, {
        value: _registerThreadGroup2
      });
      Object.defineProperty(this, _ensureInitialized, {
        value: _ensureInitialized2
      });
      Object.defineProperty(this, _strict, {
        writable: true,
        value: void 0
      });
      Object.defineProperty(this, _dispatchers, {
        writable: true,
        value: {}
      });
      Object.defineProperty(this, _maxConcurrency, {
        writable: true,
        value: void 0
      });
      Object.defineProperty(this, _defaultThreadName, {
        writable: true,
        value: void 0
      });
      Object.defineProperty(this, _threadGroups, {
        writable: true,
        value: void 0
      });
      Object.defineProperty(this, _names, {
        writable: true,
        value: new IntervalTree__default["default"]()
      });
      Object.defineProperty(this, _freeThreadId, {
        writable: true,
        value: 1
      });
      Object.defineProperty(this, _initialized, {
        writable: true,
        value: false
      });
      _classPrivateFieldLooseBase(this, _defaultThreadName)[_defaultThreadName] = options.defaultThreadName;
      _classPrivateFieldLooseBase(this, _maxConcurrency)[_maxConcurrency] = options.maxConcurrency;
      _classPrivateFieldLooseBase(this, _strict)[_strict] = options.strict;
      _classPrivateFieldLooseBase(this, _threadGroups)[_threadGroups] = options.threadGroups;
    }
    name(tid) {
      _classPrivateFieldLooseBase(this, _ensureInitialized)[_ensureInitialized]();
      if (tid === 0) {
        return _classPrivateFieldLooseBase(this, _defaultThreadName)[_defaultThreadName];
      }
      return _classPrivateFieldLooseBase(this, _names)[_names].search([tid, tid])[0];
    }
    resolve(ph, tid) {
      _classPrivateFieldLooseBase(this, _ensureInitialized)[_ensureInitialized]();
      if (tid == null) {
        return 0;
      }
      if (typeof tid === 'number') {
        return tid;
      }
      const dispatcher = _classPrivateFieldLooseBase(this, _resolveDispatcher)[_resolveDispatcher](tid);
      if (!dispatcher) {
        return new Error(`Unknown thread group "${_classPrivateFieldLooseBase(this, _resolveAlias)[_resolveAlias](tid)}"`);
      }
      const id = _classPrivateFieldLooseBase(this, _resolveId)[_resolveId](tid);
      switch (ph) {
        case 'B':
          {
            return dispatcher.begin(id);
          }
        case 'E':
          {
            return dispatcher.end(id);
          }
        default:
          {
            return dispatcher.resolve(id);
          }
      }
    }
  }
  function _ensureInitialized2() {
    if (!_classPrivateFieldLooseBase(this, _initialized)[_initialized]) {
      _classPrivateFieldLooseBase(this, _initialized)[_initialized] = true;
      for (const group of _classPrivateFieldLooseBase(this, _threadGroups)[_threadGroups]) {
        _classPrivateFieldLooseBase(this, _registerThreadGroup)[_registerThreadGroup](group);
      }
    }
  }
  function _registerThreadGroup2(config) {
    var _config$maxConcurrenc;
    const maxConcurrency = (_config$maxConcurrenc = config.maxConcurrency) != null ? _config$maxConcurrenc : _classPrivateFieldLooseBase(this, _maxConcurrency)[_maxConcurrency];
    const min = _classPrivateFieldLooseBase(this, _freeThreadId)[_freeThreadId];
    const max = min + maxConcurrency - 1;
    _classPrivateFieldLooseBase(this, _dispatchers)[_dispatchers][config.id] = new ThreadDispatcher(config.displayName, _classPrivateFieldLooseBase(this, _strict)[_strict], min, max);
    _classPrivateFieldLooseBase(this, _names)[_names].insert([min, max], config.displayName);
    _classPrivateFieldLooseBase(this, _freeThreadId)[_freeThreadId] = max + 1;
    return this;
  }
  function _resolveDispatcher2(threadAlias) {
    const groupName = typeof threadAlias === 'string' ? threadAlias : threadAlias[0];
    return _classPrivateFieldLooseBase(this, _ensureGroupDispatcher)[_ensureGroupDispatcher](groupName);
  }
  function _resolveAlias2(threadAlias) {
    return Array.isArray(threadAlias) ? threadAlias[0] : threadAlias;
  }
  function _resolveId2(threadAlias) {
    return threadAlias === undefined || typeof threadAlias === 'string' ? undefined : threadAlias[1];
  }
  function _ensureGroupDispatcher2(threadGroup) {
    if (!_classPrivateFieldLooseBase(this, _dispatchers)[_dispatchers][threadGroup] && !_classPrivateFieldLooseBase(this, _strict)[_strict]) {
      _classPrivateFieldLooseBase(this, _registerThreadGroup)[_registerThreadGroup]({
        id: threadGroup,
        displayName: threadGroup
      });
    }
    return _classPrivateFieldLooseBase(this, _dispatchers)[_dispatchers][threadGroup];
  }

  function bunyan2trace(record) {
    if (!record.ph) {
      return buildFallbackEvent(record);
    }
    switch (record.ph) {
      case 'B':
        return buildDurationBeginEvent(record);
      case 'E':
        return buildDurationEndEvent(record);
      case 'i':
        return buildInstantEvent(record);
      case 'b':
      case 'e':
      case 'n':
        return buildAsyncEvent(record);
      case 'X':
        return buildCompleteEvent(record);
      case 'C':
        return buildCounterEvent(record);
      case 'M':
        return buildMetadataEvent(record);
      default:
        return buildFallbackEvent(record);
    }
  }
  function buildAsyncEvent(record) {
    const event = bunyan2trace(record);
    return moveProperties(event.args, event, ['id', 'id2', 'scope']);
  }
  function buildCompleteEvent(record) {
    const event = extractEventWithStack(record);
    return moveProperties(event.args, event, ['dur', 'tdur', 'esf', 'estack']);
  }
  function buildCounterEvent(record) {
    const event = bunyan2trace(record);
    delete event.cat;
    return moveProperties(event.args, event, ['id']);
  }
  function buildDurationBeginEvent(record) {
    return extractEventWithStack(record);
  }
  function buildDurationEndEvent(record) {
    const event = extractEventWithStack(record);
    delete event.name;
    delete event.cat;
    return event;
  }
  function buildMetadataEvent(record) {
    const event = bunyan2trace(record);
    delete event.cat;
    return event;
  }
  function buildInstantEvent(record) {
    const event = extractEventWithStack(record);
    const args = moveProperties(event.args, event, ['s']);
    if (args.s === 'g' || args.s === 'p') {
      delete event.sf;
      delete event.stack;
    }
    return event;
  }
  function buildFallbackEvent(record) {
    const event = buildInstantEvent(record);
    event.ph = 'i';
    return event;
  }
  function extractTraceEvent(record) {
    const {
      cat,
      cname,
      ph,
      tts,
      pid,
      tid,
      time,
      msg: name,
      name: _processName,
      hostname: _hostname,
      ...args
    } = record;
    const ts = new Date(time).getTime() * 1e3;
    return {
      cat,
      cname,
      ph,
      ts,
      tts,
      pid,
      tid,
      name,
      args
    };
  }
  function extractEventWithStack(record) {
    const event = extractTraceEvent(record);
    return moveProperties(event.args, event, ['sf', 'stack']);
  }
  function moveProperties(source, target, keys) {
    for (const key of keys) {
      if (source[key] !== undefined) {
        target[key] = source[key];
        delete source[key];
      }
    }
    return target;
  }

  class StreamEventBuilder extends TEL__namespace.AbstractEventBuilder {
    constructor(stream) {
      super();
      this.stream = void 0;
      this.stream = stream;
    }
    send(event) {
      this.stream.push(event);
    }
  }

  function normalizeOptions(options) {
    var _options$ignoreFields, _options$defaultThrea, _options$maxConcurren, _options$strict, _options$threadGroups;
    options.ignoreFields = (_options$ignoreFields = options.ignoreFields) != null ? _options$ignoreFields : ['v', 'hostname', 'level', 'name'];
    options.defaultThreadName = (_options$defaultThrea = options.defaultThreadName) != null ? _options$defaultThrea : 'Main Thread';
    options.maxConcurrency = (_options$maxConcurren = options.maxConcurrency) != null ? _options$maxConcurren : 100;
    options.strict = (_options$strict = options.strict) != null ? _options$strict : false;
    options.threadGroups = Array.isArray(options.threadGroups) ? options.threadGroups.map((threadGroup, index) => typeof threadGroup === 'string' ? {
      id: threadGroup,
      displayName: threadGroup
    } : validateThreadGroup(threadGroup, index)) : (_options$threadGroups = options.threadGroups) != null ? _options$threadGroups : [];
    if (options.maxConcurrency < 1) {
      throw new Error(`maxConcurrency must be at least 1, got ${options.maxConcurrency}`);
    }
    return options;
  }
  function validateThreadGroup(threadGroup, index) {
    if (!threadGroup.id) {
      throw new Error('Missing thread group ID in thread group at index ' + index);
    }
    if (threadGroup.maxConcurrency != null) {
      if (threadGroup.maxConcurrency < 1) {
        throw new Error(`Max concurrency (${threadGroup.id} -> ${threadGroup.maxConcurrency}) has to be a positive integer`);
      }
      if (threadGroup.maxConcurrency > 1e6) {
        throw new Error(`Max concurrency (${threadGroup.id} -> ${threadGroup.maxConcurrency}) cannot be greater than 1,000,000`);
      }
    }
    return threadGroup;
  }

  var _knownTids = /*#__PURE__*/_classPrivateFieldLooseKey("knownTids");
  var _threadGroupDispatcher = /*#__PURE__*/_classPrivateFieldLooseKey("threadGroupDispatcher");
  var _builder = /*#__PURE__*/_classPrivateFieldLooseKey("builder");
  var _ignoreFields = /*#__PURE__*/_classPrivateFieldLooseKey("ignoreFields");
  var _started = /*#__PURE__*/_classPrivateFieldLooseKey("started");
  class BunyanTraceEventStream extends node_stream.Transform {
    constructor(userOptions = {}) {
      var _options$strict, _options$defaultThrea, _options$maxConcurren;
      super({
        objectMode: true
      });
      Object.defineProperty(this, _knownTids, {
        writable: true,
        value: new Set()
      });
      Object.defineProperty(this, _threadGroupDispatcher, {
        writable: true,
        value: void 0
      });
      Object.defineProperty(this, _builder, {
        writable: true,
        value: new StreamEventBuilder(this)
      });
      Object.defineProperty(this, _ignoreFields, {
        writable: true,
        value: void 0
      });
      Object.defineProperty(this, _started, {
        writable: true,
        value: false
      });
      const options = normalizeOptions(userOptions);
      _classPrivateFieldLooseBase(this, _ignoreFields)[_ignoreFields] = options.ignoreFields;
      _classPrivateFieldLooseBase(this, _threadGroupDispatcher)[_threadGroupDispatcher] = new ThreadGroupDispatcher({
        strict: (_options$strict = options.strict) != null ? _options$strict : false,
        defaultThreadName: (_options$defaultThrea = options.defaultThreadName) != null ? _options$defaultThrea : 'Main Thread',
        maxConcurrency: (_options$maxConcurren = options.maxConcurrency) != null ? _options$maxConcurren : 100,
        threadGroups: options.threadGroups
      });
    }
    _transform(record, _encoding, callback) {
      const json = typeof record === 'string' ? JSON.parse(record) : record;
      const event = json && bunyan2trace(json);
      if (event.args) {
        for (const field of _classPrivateFieldLooseBase(this, _ignoreFields)[_ignoreFields]) {
          delete event.args[field];
        }
      }
      if (!_classPrivateFieldLooseBase(this, _started)[_started]) {
        _classPrivateFieldLooseBase(this, _started)[_started] = true;
        _classPrivateFieldLooseBase(this, _builder)[_builder].metadata({
          pid: event.pid,
          ts: event.ts,
          name: 'process_name',
          args: {
            name: json.name
          }
        });
      }
      const tid = event.tid = _classPrivateFieldLooseBase(this, _threadGroupDispatcher)[_threadGroupDispatcher].resolve(event.ph, event.tid);
      if (isError(tid)) {
        callback(tid);
        return;
      }
      if (!_classPrivateFieldLooseBase(this, _knownTids)[_knownTids].has(tid)) {
        _classPrivateFieldLooseBase(this, _knownTids)[_knownTids].add(tid);
        const threadName = _classPrivateFieldLooseBase(this, _threadGroupDispatcher)[_threadGroupDispatcher].name(tid);
        if (threadName) {
          _classPrivateFieldLooseBase(this, _builder)[_builder].metadata({
            pid: event.pid,
            tid: event.tid,
            ts: event.ts,
            name: 'thread_name',
            args: {
              name: threadName
            }
          });
        }
      }
      _classPrivateFieldLooseBase(this, _builder)[_builder].send(event);
      callback(null);
    }
  }

  function jsonlReadFile(filePath) {
    return fs__default["default"].createReadStream(filePath, {
      encoding: 'utf8'
    }).pipe(StreamArray__default["default"].withParser()).pipe(new MapValues(filePath));
  }
  class MapValues extends node_stream.Transform {
    constructor(filePath) {
      super({
        objectMode: true
      });
      this.filePath = void 0;
      this.filePath = filePath;
    }
    _transform(record, _encoding, callback) {
      this.push({
        ...record,
        filePath: this.filePath
      });
      callback();
    }
  }

  function jsonlWriteFile(filePath) {
    return new JSONLFileStream({
      filePath
    });
  }
  class JSONLFileStream extends node_stream.Writable {
    constructor(options) {
      super({
        objectMode: true
      });
      this._filePath = void 0;
      this._fileDescriptor = Number.NaN;
      this._offset = Number.NaN;
      this._counter = 0;
      this._filePath = options.filePath;
    }
    _construct(callback) {
      this._offset = 0;
      this._fileDescriptor = fs__default["default"].openSync(this._filePath, 'wx');
      const content = Buffer.from('[]\n');
      fs__default["default"].write(this._fileDescriptor, content, this._offset, content.length, error => {
        if (error) {
          callback(error);
        } else {
          this._offset += 1;
          callback();
        }
      });
    }
    _write(chunk, _, callback) {
      const content = this._counter++ > 0 ? `,\n${JSON.stringify(chunk)}]\n` : `${JSON.stringify(chunk)}]\n`;
      const buffer = Buffer.from(content);
      fs__default["default"].write(this._fileDescriptor, buffer, 0, buffer.length, this._offset, (error, bytesWritten) => {
        if (error) {
          callback(error);
        } else {
          this._offset += bytesWritten - 2;
          callback();
        }
      });
    }
    _final(callback) {
      fs__default["default"].close(this._fileDescriptor, callback);
    }
  }

  var _children = /*#__PURE__*/_classPrivateFieldLooseKey("children");
  var _map = /*#__PURE__*/_classPrivateFieldLooseKey("map");
  class ArrayTreeNode {
    constructor(value) {
      this.value = void 0;
      this.index = -1;
      this.parent = void 0;
      Object.defineProperty(this, _children, {
        writable: true,
        value: []
      });
      Object.defineProperty(this, _map, {
        writable: true,
        value: new Map()
      });
      this.value = value;
    }
    get size() {
      return _classPrivateFieldLooseBase(this, _children)[_children].length;
    }
    [Symbol.iterator]() {
      return _classPrivateFieldLooseBase(this, _children)[_children][Symbol.iterator]();
    }
    findByValue(value) {
      return _classPrivateFieldLooseBase(this, _map)[_map].get(value);
    }
    appendChild(node) {
      node.index = this.size;
      node.parent = this;
      _classPrivateFieldLooseBase(this, _children)[_children].push(node);
      _classPrivateFieldLooseBase(this, _map)[_map].set(node.value, node);
      return node;
    }
  }

  var _min = /*#__PURE__*/_classPrivateFieldLooseKey("min");
  var _max = /*#__PURE__*/_classPrivateFieldLooseKey("max");
  class RangeTreeNode {
    constructor(value) {
      this.value = void 0;
      this.index = -1;
      this.parent = void 0;
      Object.defineProperty(this, _min, {
        writable: true,
        value: Number.POSITIVE_INFINITY
      });
      Object.defineProperty(this, _max, {
        writable: true,
        value: Number.NEGATIVE_INFINITY
      });
      this.value = value;
    }
    get min() {
      return _classPrivateFieldLooseBase(this, _min)[_min];
    }
    get max() {
      return _classPrivateFieldLooseBase(this, _max)[_max];
    }
    get size() {
      return _classPrivateFieldLooseBase(this, _max)[_max] - _classPrivateFieldLooseBase(this, _min)[_min] + 1;
    }
    add(child) {
      if (child < _classPrivateFieldLooseBase(this, _min)[_min]) {
        _classPrivateFieldLooseBase(this, _min)[_min] = child;
      }
      if (child > _classPrivateFieldLooseBase(this, _max)[_max]) {
        _classPrivateFieldLooseBase(this, _max)[_max] = child;
      }
    }
  }

  class FileNode extends RangeTreeNode {
    constructor(...args) {
      super(...args);
      this.rank = void 0;
      this.offset = void 0;
    }
    addTID(tid) {
      return super.add(tid);
    }
    transpose(tid) {
      var _this$offset;
      if (tid < this.min || tid > this.max) {
        throw new Error(`Value ${tid} not found in range: [${this.min}, ${this.max}]`);
      }
      return ((_this$offset = this.offset) != null ? _this$offset : 0) + (tid - this.min);
    }
  }

  class PIDNode extends ArrayTreeNode {
    addFile(file) {
      var _this$findByValue;
      return (_this$findByValue = this.findByValue(file)) != null ? _this$findByValue : this.appendChild(new FileNode(file));
    }
  }

  class RootNode extends ArrayTreeNode {
    constructor() {
      super(undefined);
    }
    addPID(pid) {
      var _this$findByValue;
      return (_this$findByValue = this.findByValue(pid)) != null ? _this$findByValue : this.appendChild(new PIDNode(pid));
    }
    rank() {
      let index = 0;
      let offset = 0;
      for (const pid of this) {
        for (const file of pid) {
          file.rank = index++;
          file.offset = offset;
          offset += file.size;
        }
      }
    }
  }

  class PIDResolver {
    constructor() {
      this.tree = new RootNode();
    }
    add(pid, _filePath, tid) {
      this.tree.addPID(pid).addFile('').addTID(tid);
    }
    finalize() {
      this.tree.rank();
    }
    resolvePid(_filePath, pid) {
      return pid;
    }
    resolveTid(_filePath, pid, tid) {
      var _$file$transpose;
      const $pid = this.tree.findByValue(pid);
      const $file = $pid == null ? void 0 : $pid.findByValue('');
      return (_$file$transpose = $file == null ? void 0 : $file.transpose(tid)) != null ? _$file$transpose : Number.NaN;
    }
  }

  class FilePIDResolver extends PIDResolver {
    add(pid, filePath, tid) {
      this.tree.addPID(pid).addFile(filePath).addTID(tid);
    }
    resolvePid(filePath, pid) {
      var _$file$rank;
      const $pid = this.tree.findByValue(pid);
      const $file = $pid == null ? void 0 : $pid.findByValue(filePath);
      return ((_$file$rank = $file == null ? void 0 : $file.rank) != null ? _$file$rank : Number.NaN) + 1;
    }
    resolveTid(filePath, pid, tid) {
      var _$file$transpose;
      const $pid = this.tree.findByValue(pid);
      const $file = $pid == null ? void 0 : $pid.findByValue(filePath);
      return (_$file$transpose = $file == null ? void 0 : $file.transpose(tid)) != null ? _$file$transpose : Number.NaN;
    }
  }

  function multisort(streams) {
    return multiSortStream__default["default"](streams, comparator);
  }
  function comparator(a, b) {
    const {
      value: aa
    } = a;
    const {
      value: bb
    } = b;
    return aa.ts < bb.ts ? -1 : aa.ts > bb.ts ? 1 : 0;
  }

  var _resolver$1 = /*#__PURE__*/_classPrivateFieldLooseKey("resolver");
  class TraceAnalyze extends node_stream.Writable {
    constructor(resolver) {
      super({
        objectMode: true,
        highWaterMark: Number.MAX_SAFE_INTEGER
      });
      Object.defineProperty(this, _resolver$1, {
        writable: true,
        value: void 0
      });
      _classPrivateFieldLooseBase(this, _resolver$1)[_resolver$1] = resolver;
    }
    _write(chunk, _encoding, callback) {
      const entry = chunk;
      _classPrivateFieldLooseBase(this, _resolver$1)[_resolver$1].add(entry.value.pid, entry.filePath, entry.value.tid);
      callback();
    }
    _final(callback) {
      _classPrivateFieldLooseBase(this, _resolver$1)[_resolver$1].finalize();
      callback();
    }
  }

  var _resolverPromise = /*#__PURE__*/_classPrivateFieldLooseKey("resolverPromise");
  var _resolver = /*#__PURE__*/_classPrivateFieldLooseKey("resolver");
  class TraceMerge extends node_stream.Transform {
    constructor(resolverPromise) {
      super({
        objectMode: true,
        highWaterMark: Number.MAX_SAFE_INTEGER
      });
      Object.defineProperty(this, _resolverPromise, {
        writable: true,
        value: void 0
      });
      Object.defineProperty(this, _resolver, {
        writable: true,
        value: void 0
      });
      _classPrivateFieldLooseBase(this, _resolverPromise)[_resolverPromise] = resolverPromise;
    }
    _transform(chunk, _encoding, callback) {
      if (_classPrivateFieldLooseBase(this, _resolver)[_resolver]) {
        const entry = chunk;
        const output = {
          ...entry.value
        };
        if (output.pid != null) {
          output.pid = _classPrivateFieldLooseBase(this, _resolver)[_resolver].resolvePid(entry.filePath, entry.value.pid);
        }
        if (output.tid != null) {
          output.tid = _classPrivateFieldLooseBase(this, _resolver)[_resolver].resolveTid(entry.filePath, entry.value.pid, entry.value.tid);
        }
        this.push(output);
        callback();
      } else {
        _classPrivateFieldLooseBase(this, _resolverPromise)[_resolverPromise].then(resolver => {
          _classPrivateFieldLooseBase(this, _resolver)[_resolver] = resolver;
          this._transform(chunk, _encoding, callback);
        }, error => {
          callback(error);
        });
      }
    }
  }

  function traceMerge(filePaths, options) {
    const streams = filePaths.map(filePath => jsonlReadFile(filePath));
    const resolver = makeResolver(options);
    const $resolver = makeDeferred();
    const analyze = new TraceAnalyze(resolver).on('error', error => $resolver.reject(error)).on('finish', () => $resolver.resolve(resolver));
    const merge = new TraceMerge($resolver.promise);
    const sorted = multisort(streams);
    sorted.pipe(analyze);
    return sorted.pipe(merge);
  }
  function makeResolver(options) {
    return (options == null ? void 0 : options.mode) === 'fpid' ? new FilePIDResolver() : new PIDResolver();
  }
  function makeDeferred() {
    let resolve;
    let reject;
    const promise = new Promise((_resolve, _reject) => {
      resolve = _resolve;
      reject = _reject;
    });
    return {
      promise: promise,
      resolve: resolve,
      reject: reject
    };
  }

  function traceEventStream(options) {
    const jsonl = jsonlWriteFile(options.filePath);
    const stream = new BunyanTraceEventStream(options);
    stream.pipe(jsonl);
    return stream;
  }

  function uniteTraceEvents(sourcePaths, options) {
    return traceMerge(sourcePaths, options);
  }
  async function uniteTraceEventsToFile(sourcePaths, destinationPath, options) {
    return new Promise((resolve, reject) => {
      uniteTraceEvents(sourcePaths, options).pipe(jsonlWriteFile(destinationPath)).on('finish', resolve).on('error', reject);
    });
  }

  function wrapLogger(maybeLogger, maybeConfig) {
    var _maybeLogger$logger;
    const logger = (_maybeLogger$logger = maybeLogger.logger) != null ? _maybeLogger$logger : maybeLogger;
    const config = logger === maybeLogger ? maybeConfig : maybeLogger;
    return new Bunyamin({
      ...config,
      logger
    });
  }

  const bunyamin = realm.bunyamin;
  const nobunyamin = realm.nobunyamin;
  const threadGroups = realm.threadGroups;

  exports.NoopLogger = NoopLogger;
  exports.bunyamin = bunyamin;
  exports["default"] = bunyamin;
  exports.isDebug = isDebug;
  exports.nobunyamin = nobunyamin;
  exports.noopLogger = noopLogger;
  exports.threadGroups = threadGroups;
  exports.traceEventStream = traceEventStream;
  exports.uniteTraceEvents = uniteTraceEvents;
  exports.uniteTraceEventsToFile = uniteTraceEventsToFile;
  exports.wrapLogger = wrapLogger;

}));
//# sourceMappingURL=index.umd.js.map
