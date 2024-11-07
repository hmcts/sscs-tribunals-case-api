const { test } = require('uvu');
const assert = require('uvu/assert');

const mime = require('../src');

test('getType - check standards', () => {
  assert.is(mime.getType('abc.doc'), 'application/msword');
});

test('getType - check others', () => {
  assert.is(mime.getType('abc.xls'), 'application/vnd.ms-excel');
});

test('getExtension - check standards', () => {
  assert.is(mime.getExtension('application/json'), 'json');
});

test('getExtension - check standards', () => {
  assert.is(mime.getExtension('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'), 'xlsx');
});

test.run();