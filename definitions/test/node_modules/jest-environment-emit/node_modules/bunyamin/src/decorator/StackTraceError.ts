export class StackTraceError extends Error {
  constructor() {
    super('Providing stack trace below:');
    // eslint-disable-next-line unicorn/custom-error-definition
    this.name = 'StackTrace';
  }

  static empty() {
    return {
      message: '',
      stack: '',
    };
  }
}
