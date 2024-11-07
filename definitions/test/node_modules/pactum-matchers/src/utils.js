const { setRules } = require('./rules');
const { getValue } = require('./value');
const { compare: _compare } = require('./compare');

function setMatchingRules(rules, data, path) {
  return setRules(rules, data, path);
}

function compare(actual, expected, rules, path, strict) {
  try {
    _compare(actual, expected, rules, path);
    if (strict) {
      _compare(expected, actual, rules, path, true);
    }
  } catch (error) {
    return {
      equal: false,
      message: error.toString()
    };
  }
  return {
    equal: true,
    message: ''
  };
}

module.exports = {
  setMatchingRules,
  getValue,
  compare
};