const utils = require('./utils');

const ISO8601_DATE_PATTERN = "^([\\+-]?\\d{4}(?!\\d{2}\\b))((-?)((0[1-9]|1[0-2])(\\3([12]\\d|0[1-9]|3[01]))?|W([0-4]\\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]\\d|6[1-6])))?)$";
const ISO8601_DATE_TIME_PATTERN = "^\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d([+-][0-2]\\d:[0-5]\\d|Z)$";
const ISO8601_DATE_TIME_MS_PATTERN = "^\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d\\.\\d+([+-][0-2]\\d(:?[0-5]\\d)?|Z)$";
const RFC3339_TIMESTAMP_PATTERN = /^(\d+)-(0[1-9]|1[012])-(0[1-9]|[12]\d|3[01])\s([01]\d|2[0-3]):([0-5]\d):([0-5]\d|60)(\.\d+)?(([Zz])|([\+|\-]([01]\d|2[0-3])))$/;

function like(value) {
  return {
    value,
    pactum_type: 'LIKE'
  };
}

function eachLike(content, options) {
  let min = 1;
  let value = [content];
  let items = [];
  if (typeof options === 'object') {
    if (options.items) {
      items = options.items;
    }
    if (typeof options.min === 'number') {
      min = options.min;
    }
  }
  return {
    value,
    items,
    min,
    pactum_type: 'ARRAY_LIKE'
  };
}

function oneOf(value) {
  return {
    value,
    pactum_type: 'ONE_OF'
  };
}

function expression(value, expr) {
  if (typeof expr === 'undefined') {
    expr = value;
    value = 'expression'
  }
  return {
    value,
    expr,
    pactum_type: 'EXPR'
  };
}

function string(value) {
  return {
    value: typeof value === 'undefined' ? 'string' : value,
    pactum_type: 'STRING'
  };
}

function regex(value, matcher) {
  if (typeof matcher === 'undefined') {
    matcher = value;
    value = 'regex';
  }
  if (matcher instanceof RegExp) {
    matcher = matcher.source;
  }
  try {
    new RegExp(matcher);
  } catch (error) {
    throw `Invalid RegEx Matcher - ${matcher}`;
  }
  return {
    value,
    matcher,
    pactum_type: 'REGEX'
  };
}

function includes(value) {
  return regex(value, value);
}

function email(value) {
  return {
    value: value || 'hello@pactum.js',
    pactum_type: 'EMAIL'
  };
}

function uuid(value) {
  return {
    value: value || 'ce118b6e-d8e1-11e7-9296-cec278b6b50a',
    pactum_type: 'UUID'
  };
}

function date(value) {
  return regex(value || '2020-12-12', ISO8601_DATE_PATTERN);
}

function dateTime(value) {
  return regex(value || '2020-12-12T16:53:10+01:00', ISO8601_DATE_TIME_PATTERN);
}

function dateTimeMs(value) {
  return regex({
    value: value || '2020-12-12T16:53:10+01:00',
    matcher: ISO8601_DATE_TIME_MS_PATTERN
  });
}

function timestamp(value) {
  return regex({
    value: value || '2020-12-12 16:41:41.090Z',
    matcher: RFC3339_TIMESTAMP_PATTERN
  });
}

function any(value) {
  return {
    value: value,
    pactum_type: 'ANY'
  };
}

function int(value) {
  return {
    value: value || 123,
    pactum_type: 'INT'
  };
}

function float(value) {
  return {
    value: value || 123.456,
    pactum_type: 'FLOAT'
  };
}

function gt(value) {
  return {
    value: value,
    pactum_type: 'GT'
  };
}

function gte(value) {
  return {
    value: value,
    pactum_type: 'GTE'
  };
}

function lt(value) {
  return {
    value: value,
    pactum_type: 'LT'
  };
}

function lte(value) {
  return {
    value: value,
    pactum_type: 'LTE'
  };
}

function notIncludes(value) {
  return {
    value: value,
    pactum_type: 'NOT_INCLUDES'
  };
}

function notNull(value) {
  return {
    value: value,
    pactum_type: 'NOT_NULL'
  };
}

function notEquals(value) {
  return {
    value: value,
    pactum_type: 'NOT_EQUALS'
  };
}

function iso(value) {
  return {
    value: value || "2022-02-21T21:28:00.000Z",
    pactum_type: 'ISO'
  };
}

module.exports = {
  like,
  eachLike,
  oneOf,
  expression,
  string,
  regex,
  includes,
  email,
  uuid,
  date,
  dateTime,
  dateTimeMs,
  timestamp,
  any,
  int,
  float,
  gt,
  gte,
  lt,
  lte,
  notIncludes,
  notNull,
  notEquals,
  iso,
  utils
};