import type { ThreadGroupConfig } from '../threads';
import type { TraceEventStreamOptions } from './TraceEventStreamOptions';

export function normalizeOptions(
  options: TraceEventStreamOptions,
): Required<TraceEventStreamOptions> {
  options.ignoreFields = options.ignoreFields ?? ['v', 'hostname', 'level', 'name'];
  options.defaultThreadName = options.defaultThreadName ?? 'Main Thread';
  options.maxConcurrency = options.maxConcurrency ?? 100;
  options.strict = options.strict ?? false;
  options.threadGroups = Array.isArray(options.threadGroups)
    ? options.threadGroups.map((threadGroup, index) =>
        typeof threadGroup === 'string'
          ? {
              id: threadGroup,
              displayName: threadGroup,
            }
          : validateThreadGroup(threadGroup, index),
      )
    : options.threadGroups ?? [];

  if (options.maxConcurrency < 1) {
    throw new Error(`maxConcurrency must be at least 1, got ${options.maxConcurrency}`);
  }

  return options as Required<TraceEventStreamOptions>;
}

function validateThreadGroup(threadGroup: ThreadGroupConfig, index: number) {
  if (!threadGroup.id) {
    throw new Error('Missing thread group ID in thread group at index ' + index);
  }

  if (threadGroup.maxConcurrency != null) {
    if (threadGroup.maxConcurrency < 1) {
      throw new Error(
        `Max concurrency (${threadGroup.id} -> ${threadGroup.maxConcurrency}) has to be a positive integer`,
      );
    }

    if (threadGroup.maxConcurrency > 1e6) {
      throw new Error(
        `Max concurrency (${threadGroup.id} -> ${threadGroup.maxConcurrency}) cannot be greater than 1,000,000`,
      );
    }
  }

  return threadGroup;
}
