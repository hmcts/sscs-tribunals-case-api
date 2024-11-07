const defaults = require('./index');

module.exports = {
  extends: defaults.extends,
  rules: Object.assign({}, defaults.rules, {
    'arrow-body-style': 'off',
    'newline-per-chained-call': 'off',
    'max-nested-callbacks': ['error', 5],
    'no-undefined': 'off',
    'no-magic-numbers': 'off',
    'no-unused-expressions': 'off',
    'max-lines': ['error', 600],
  })
}
