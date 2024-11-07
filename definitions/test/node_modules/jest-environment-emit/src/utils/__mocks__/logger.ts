const { bunyamin } = jest.requireActual('bunyamin');

bunyamin.useLogger({
  fatal: jest.fn(),
  error: jest.fn(),
  warn: jest.fn(),
  info: jest.fn(),
  debug: jest.fn(),
  trace: jest.fn(),
});

const optimizeTracing = jest.fn((f) => f);

export { bunyamin as logger, optimizeTracing };
