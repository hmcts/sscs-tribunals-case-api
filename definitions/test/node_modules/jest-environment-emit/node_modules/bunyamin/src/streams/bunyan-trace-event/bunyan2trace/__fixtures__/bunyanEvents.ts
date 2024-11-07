export function anEvent() {
  return {
    cat: 'lifecycle',
    cname: '#f00', // not sure about the correct color format
    custom: 'value',
    hostname: 'ignored',
    msg: 'The event name',
    name: 'ignored',
    pid: 42,
    tid: 0,
    time: '2021-01-01T00:00:00.000Z',
    tts: 1_683_314_088_734,
  };
}

export function anEventWithStack() {
  return {
    ...anEvent(),
    sf: 0,
    stack: [],
  };
}

export function anInstantEvent(s = 't'): any {
  return {
    ...anEventWithStack(),
    ph: 'i',
    s,
  };
}

export function aDurationBeginEvent(): any {
  return {
    ...anEventWithStack(),
    ph: 'B',
  };
}

export function aDurationEndEvent() {
  return {
    ...anEventWithStack(),
    ph: 'E',
  };
}
