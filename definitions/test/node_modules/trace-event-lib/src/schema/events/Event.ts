/**
 * The main data type of Chrome Trace Event format
 * @see https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU
 */
export interface Event {
  /**
   * The name of the event, as displayed in Trace Viewer
   */
  name?: string;
  /**
   * A fixed color name to associate with the event.
   * If provided, cname must be one of the names listed in trace-viewer's base color scheme's
   * [reserved color names list]{@link https://github.com/catapult-project/catapult/blob/main/tracing/tracing/base/color_scheme.html}.
   */
  cname?: string;
  /**
   * The event categories. This is a comma separated list of categories for the event.
   * The categories can be used to hide events in the Trace Viewer UI.
   */
  cat?: string;
  /**
   * The event type.
   * This is a single character which changes depending on the type of event being output.
   */
  ph: string;
  /**
   * The tracing clock timestamp of the event. The timestamps are provided at microsecond granularity.
   */
  ts: number;
  /**
   * Optional. The thread clock timestamp of the event. The timestamps are provided at microsecond granularity.
   */
  tts?: number;
  /**
   * The process ID for the process that output this event.
   */
  pid: number;
  /**
   * The thread ID for the thread that output this event.
   */
  tid: number;
  /**
   *  Any arguments provided for the event.
   *  Some of the event types have required argument fields, otherwise, you can put any information you wish in here.
   *  The arguments are displayed in Trace Viewer when you view an event in the analysis section.
   */
  args?: Record<string, unknown>;
}
