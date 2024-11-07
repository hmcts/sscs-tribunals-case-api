import { Event } from './Event';

export interface EventWithStack extends Event {
  /**
   * An id for a stackFrame object in {@see TraceEventJSONObject#stackFrames}
   */
  sf?: number;
  /**
   * A stack is just an array of strings.
   * Mutually exclusive with {@link DurationEvent#sf} property.
   * The 0th item in the array is the rootmost part of the callstack, the last item is the leafmost entry in the stack,
   * e.g. the closest to what was running when the event was issued.
   * You can put anything you want in each trace, but strings in hex form ("0x1234")
   * are treated as program counter addresses and are eligible for symbolization.
   * @example ["0x1", "0x2"]
   */
  stack?: string[];
}
