const { isPrimitive, getType } = require('./helpers');
const patterns = require('./patterns');

function compare(actual, expected, rules, path, inverse) {
  const regex_rules = getRegExRules(rules);
  return _compare(actual, expected, rules, regex_rules, path, inverse);
}

function _compare(actual, expected, rules, regex_rules, path, inverse) {
  const rule = getCurrentPathRule(rules, regex_rules, path);
  if (rule) {
    compareWithRule(actual, expected, rules, regex_rules, path, rule, inverse);
  } else {
    typeCompare(actual, expected, path);
    arrayCompare(actual, expected, rules, regex_rules, path);
    objectCompare(actual, expected, rules, regex_rules, path);
    valueCompare(actual, expected, path);
  }
  return '';
}

function escapeRegex(string) {
  return string.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
}

function getRegExRules(rules) {
  const regex_rules = {};
  Object.keys(rules).forEach(rule => {
    if (rule == '__proto__' || rule == 'constructor' || rule == 'prototype') {
      return;
    }
    let regex_rule = escapeRegex(rule);
    regex_rule = regex_rule.replace(/\\\[\\\*\\\]/g, '\\\[\\d+\\\]');
    if (regex_rule.endsWith('.\\*')) {
      regex_rules[regex_rule.slice(0, regex_rule.length - 2) + '[^.]+$'] = rules[rule];
    } else {
      regex_rules[regex_rule + '$'] = rules[rule];
    }
  });
  return regex_rules;
}

function getCurrentPathRuleUsingRegEx(regex_rules, path) {
  const rules = Object.keys(regex_rules);
  let fall_back_rule = null;
  for (let i = 0; i < rules.length; i++) {
    const rule = rules[i];
    if (rule == '__proto__' || rule == 'constructor' || rule == 'prototype') {
      return;
    }
    const rx = new RegExp(rule);
    if (rx.test(path)) {
      if (rule.endsWith('.[^.]+$')) {
        fall_back_rule = regex_rules[rule];
      } else {
        return regex_rules[rule];
      }
    }
  }
  return fall_back_rule;
}

function getCurrentPathRule(rules, regex_rules, path) {
  if (rules[path]) return rules[path];
  const genericPath = path.replace(/\[\d+\]/g, '[*]');
  if (rules[genericPath]) return rules[genericPath];
  let dotIndex = path.lastIndexOf('.');
  const allPropsPath = `${path.slice(0, dotIndex)}.*`;
  if (rules[allPropsPath]) return rules[allPropsPath];
  dotIndex = genericPath.lastIndexOf('.');
  const allPropsGenericPath = `${genericPath.slice(0, dotIndex)}.*`;
  if (rules[allPropsGenericPath]) return rules[allPropsGenericPath];
  if (rules[genericPath]) return rules[genericPath];
  return getCurrentPathRuleUsingRegEx(regex_rules, path)
}

function compareWithRule(actual, expected, rules, regex_rules, path, rule, inverse) {
  switch (rule.match) {
    case 'type':
      compareWithRuleType(actual, expected, rules, regex_rules, path, rule);
      break;
    case 'regex':
      compareWithRuleRegex(actual, rule, path, inverse);
      break;
    case 'oneOf':
      compareWithRuleOneOf(actual, rule, path);
      break;
    case 'expr':
      compareWithRuleExpr(actual, rule, path);
      break;
    case 'string':
      compareWithString(actual, rule, path);
      break;
    case 'uuid':
      compareWithUUID(actual, rule, path);
      break;
    case 'email':
      compareWithEmail(actual, rule, path);
      break;
    case 'any':
      compareWithAny(actual, rule, path);
      break;
    case 'int':
      compareWithInt(actual, rule, path);
      break;
    case 'float':
      compareWithFloat(actual, rule, path);
      break;
    case 'gt':
      compareWithGt(actual, expected, rule, path);
      break;
    case 'gte':
      compareWithGte(actual, expected, rule, path);
      break;
    case 'lt':
      compareWithLt(actual, expected, rule, path);
      break;
    case 'lte':
      compareWithLte(actual, expected, rule, path);
      break;
    case 'not_includes':
      compareNotIncludes(actual, expected, rule, path);
      break;
    case 'not_null':
      compareWithNotNull(actual, path);
      break;
    case 'not_equals':
      compareWithNotEquals(actual, expected, path);
      break;
    case 'iso':
      compareWithISO(actual, expected, path);
      break;
  }
}

function compareWithRuleType(actual, expected, rules, regex_rules, path, rule) {
  typeCompare(actual, expected, path);
  const type = getType(expected);
  if (type === 'array') {
    if (typeof rule.min !== 'undefined') {
      if (actual.length < rule.min) {
        throw `Json doesn't have 'array' with min length of '${rule.min}' at '${path}' but found 'array' with length '${actual.length}'`;
      }
      if (rule.min > 0 || typeof expected[0] !== 'undefined') {
        for (let i = 0; i < actual.length; i++) {
          _compare(actual[i], expected[0], rules, regex_rules, `${path}[${i}]`);
        }
      }
    } else {
      arrayCompare(actual, expected, rules, regex_rules, path);
    }
    
  } else if (type === 'object') {
    objectCompare(actual, expected, rules, regex_rules, path);
  }
}

function compareWithRuleRegex(actual, rule, path, inverse) {
  if (inverse) {
    return;
  }
  const regex = new RegExp(rule.regex);
  if (!regex.test(actual)) {
    throw `Json doesn't match with "${rule.regex}" at "${path}" but found "${actual}"`;
  }
}

function compareWithRuleOneOf(actual, rule, path) {
  const values = rule.value;
  let found = false;
  for (let i = 0; i < values.length; i++) {
    found = actual === values[i];
    if (found) break;
  }
  if (!found) {
    throw `Json doesn't have one of the expected values at "${path}" but found "${actual}"`;
  }
}

function compareWithRuleExpr(actual, rule, path) {
  const expr = rule.expr;
  const expression = expr.replace('$V', 'actual');
  if (eval(expression) !== true) {
    throw `Json doesn't fulfil expression '${expression.replace('actual', path).trim()}'`;
  }
}

function compareWithString(actual, rule, path) {
  const type = getType(actual);
  if (type !== 'string') {
    throw `Json doesn't have type 'string' at '${path}' but found '${type}'`;
  }
  if (actual.length === 0) {
    throw `Json have an empty string at '${path}'`;
  }
}

function compareWithUUID(actual, rule, path) {
  const pattern = patterns.UUID;
  if (!pattern.test(actual)) {
    throw `Json doesn't match with "UUID" pattern at "${path}" but found "${actual}"`;
  }
}

function compareWithAny(actual, rule, path) {
  const types = [Number, String, Boolean, Object, Symbol, null, undefined]
  const type = getType(actual);
  if (type in types) {
    throw `Json doesn't have type 'any' at '${path}' but found '${type}'`;
  }
}

function compareWithInt(actual, rule, path) {
  const type = getType(actual);
  if (type !== 'number') {
    throw `Json doesn't have type 'number' at '${path}' but found '${type}'`;
  } else {
    const pattern = patterns.INT;
    if (!pattern.test(actual)) {
      throw `Json doesn't have 'integer' number at '${path}' but found '${actual}'`;
    }
  }
}

function compareWithFloat(actual, rule, path) {
  const type = getType(actual);
  if (type !== 'number') {
    throw `Json doesn't have type 'number' at '${path}' but found '${type}'`;
  } else {
    const pattern = patterns.FLOAT;
    if (!pattern.test(actual)) {
      throw `Json doesn't have 'float' number at '${path}' but found '${actual}'`;
    }
  }
}

function compareWithGt(actual, expected, rule, path) {
  const type = getType(actual);
  if (type !== 'number') {
    throw `Json doesn't have type 'number' at '${path}' but found '${type}'`;
  } else if (!(actual > expected)) {
    throw `Json doesn't have 'greater' value than '${expected}' at '${path}' but found '${actual}'`;
  }
}

function compareWithGte(actual, expected, rule, path) {
  const type = getType(actual);
  if (type !== 'number') {
    throw `Json doesn't have type 'number' at '${path}' but found '${type}'`;
  } else if (!(actual >= expected)) {
    throw `Json doesn't have 'greater or equal' value than '${expected}' at '${path}' but found '${actual}'`;
  }
}

function compareWithLt(actual, expected, rule, path) {
  const type = getType(actual);
  if (type !== 'number') {
    throw `Json doesn't have type 'number' at '${path}' but found '${type}'`;
  } else if (!(actual < expected)) {
    throw `Json doesn't have 'lesser' value than '${expected}' at '${path}' but found '${actual}'`;
  }
}

function compareWithLte(actual, expected, rule, path) {
  const type = getType(actual);
  if (type !== 'number') {
    throw `Json doesn't have type 'number' at '${path}' but found '${type}'`;
  } else if (!(actual <= expected)) {
    throw `Json doesn't have 'lesser or equal' value than '${expected}' at '${path}' but found '${actual}'`;
  }
}

function compareWithEmail(actual, rule, path) {
  const pattern = patterns.EMAIL;
  if (!pattern.test(actual)) {
    throw `Json doesn't match with "EMAIL" pattern at "${path}" but found "${actual}"`;
  }
}

function compareNotIncludes(actual, expected_values, rule, path) {
  const expected_type = getType(expected_values);
  if (expected_type !== 'array') {
    expected_values = [expected_values];
  }
  for (const expected of expected_values) {
    const actual_type = getType(actual);
    if (actual_type === 'object') {
      if (typeof actual[expected] !== 'undefined') {
        throw `Json has a property of "${expected}" at "${path}"`;
      }
    } else if (actual_type === 'array') {
      if (actual.includes(expected)) {
        throw `Json has an element "${expected}" in an array at "${path}"`;
      }
    } else {
      throw `Json doesn't have a "object" at "${path}"`;
    }
  }
}

function compareWithNotNull(actual, path) {
  if (actual === null) {
    throw `Json has a "null" at "${path}"`;
  }
}

function compareWithNotEquals(actual, expected, path) {
  if (actual === expected) {
    throw `Json have a value '${expected}' at '${path}'`;
  }
}

function typeCompare(actual, expected, path) {
  const actualType = getType(actual);
  const expectedType = getType(expected);
  if (actualType !== expectedType) {
    throw `Json doesn't have type '${expectedType}' at '${path}' but found '${actualType}'`;
  }
}

function arrayCompare(actual, expected, rules, regex_rules, path) {
  if (getType(expected) === 'array') {
    if (actual.length !== expected.length) {
      throw `Json doesn't have 'array' with length '${expected.length}' at '${path}' but found 'array' with length '${actual.length}'`;
    }
    for (let i = 0; i < expected.length; i++) {
      _compare(actual[i], expected[i], rules, regex_rules, `${path}[${i}]`);
    }
  }
}

function objectCompare(actual, expected, rules, regex_rules, path) {
  if (getType(expected) === 'object') {
    for (const prop in expected) {
      if (!Object.prototype.hasOwnProperty.call(actual, prop)) {
        throw `Json doesn't have property '${prop}' at '${path}'`;
      }
      _compare(actual[prop], expected[prop], rules, regex_rules, `${path}.${prop}`);
    }
  }
}

function valueCompare(actual, expected, path) {
  if (isPrimitive(expected)) {
    if (actual !== expected) throw `Json doesn't have a value '${expected}' at '${path}' but found '${actual}'`;
  }
}

function compareWithISO(actual, rule, path) {
  const pattern = patterns.ISO;
  if (!pattern.test(actual)) {
    throw `Json doesn't match with "ISO" date format at "${path}" but found "${actual}"`;
  }
}

module.exports = {
  compare
};
