const { isPureObject, isObject } = require('./helpers');

function setMatchingRules(rules, data, path) {
  switch (data.pactum_type) {
    case 'LIKE':
      rules[path] = { match: 'type' };
      if (isObject(data.value)) {
        rules[`${path}.*`] = { match: 'type' };
      }
      break;
    case 'ONE_OF':
      rules[path] = { match: 'oneOf', value: data.value };
      break;
    case 'EXPR':
      rules[path] = { match: 'expr', expr: data.expr };
      break;
    case 'REGEX':
      rules[path] = { match: 'regex', regex: data.matcher };
      break;
    case 'ARRAY_LIKE':
      rules[path] = { match: 'type', min: data.min };
      rules[`${path}[*]`] = { match: 'type' };
      if (isObject(data.value)) {
        rules[`${path}[*].*`] = { match: 'type' };
      }
      break;
    case 'ANY':
      rules[path] = { match: 'any' };
      break;
    default:
      rules[path] = { match: data.pactum_type.toLowerCase() };
      break;
  }
}

function setRules(rules, data, path) {
  if (Array.isArray(data)) {
    for (let i = 0; i < data.length; i++) {
      const item = data[i];
      if (rules[`${path}[*]`]) {
        setRules(rules, item, `${path}[*]`);
      } else {
        setRules(rules, item, `${path}[${i}]`);
      }
    }
  } else if (isPureObject(data)) {
    if (data.pactum_type) {
      setMatchingRules(rules, data, path);
      if (isObject(data.value)) {
        setRules(rules, data.value, path);
      }
    } else {
      for (const prop in data) {
        setRules(rules, data[prop], `${path}.${prop}`);
      }
    }
  }
  return rules;
}

module.exports = {
  setRules
};