import { Event } from './events/Event';
import { GlobalSample, StackFrame, StackFrameId } from './misc';

export type TraceEventJSONObject = {
  /**
   * Essentially, an array of event objects. The events do not have to be in timestamp-sorted order.
   */
  traceEvents: Event[];
  /**
   * String that specifies in which unit timestamps should be displayed.
   * @default 'ms'
   */
  displayTimeUnit?: 'ms' | 'ns';
  /**
   * String of Linux ftrace data or Windows ETW trace data.
   * This data must start with # tracer: and adhere to the Linux ftrace format or adhere to Windows ETW format.
   * @see http://lwn.net/Articles/365835/
   */
  systemTraceEvents?: string;
  /**
   * Any other properties seen in the object, in this case otherData are assumed to be metadata for the trace.
   * They will be collected and stored in an array in the trace model.
   * This metadata is accessible through the Metadata button in Trace Viewer.
   */
  otherData?: Record<string, unknown>;
  /**
   * String of BattOr power data.
   */
  powerTraceAsString?: string;
  /**
   * Dictionary of stack frames, their ids, and their parents that allows compact representation of stack traces throughout the rest of the trace file.
   * It is optional but sometimes very useful in shrinking file sizes.
   */
  stackFrames?: Record<StackFrameId, StackFrame>;
  /**
   * Stores sampling profiler data from a OS level profiler.
   * The stored samples are different from trace event samples, and is meant to augment the traceEvent data with lower level information.
   * It is OK to have a trace event file with just sample data, but in that case {@link TraceEventJSONObject#traceEvents}
   * must still be provided and set to [].
   */
  samples?: GlobalSample[];
};

export type TraceEventJSONArray = Event[];

export type TraceEventJSON = TraceEventJSONArray | TraceEventJSONObject;
