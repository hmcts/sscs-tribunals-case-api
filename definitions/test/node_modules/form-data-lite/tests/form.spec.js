const { test } = require('uvu');
const assert = require('uvu/assert');

const FormData = require('../src');

test('FormData - getHeaders should have content-type as multipart', () => {
  const form = new FormData();
  form.getHeaders()
  assert.match(form.getHeaders()["content-type"], 'multipart/form-data');
});

test('FormData - should be able to append data', () => {
  const form = new FormData();
  form.append('key', 'value');
});

test.run();