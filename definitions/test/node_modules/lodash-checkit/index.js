var lodash = require('lodash');
var Checkit = require('checkit');

var lodashNativeDict = {};
var checkitRegexDict = {};
var checkitOtherDict = {};
var lodashCaseDict = {};
var notDict = {};

var checkitMixins = {};

Object.keys(lodash).filter(function (fn) { return /^is/.test(fn); }).forEach(function (key) {
  lodashNativeDict[key] = 'lodash [' + key + '](https://lodash.com/docs/#' + key + ')';
});

var validatorInstance = new Checkit.Validator(null, {language: 'en'});
var checkitKeys = Object.keys(Checkit.Regex).concat(Object.keys(Checkit.Validator.prototype));
checkitKeys.forEach(function (k) {
  var testFn;
  if (k === 'accepted') return;
  if (k === 'matchesField') return;
  var fnName = 'is' + lodash.upperFirst(k);
  if (lodashNativeDict[fnName]) return;
  var re = Checkit.Regex[k];
  if (fnName === 'isContains') fnName = 'isContainerFor'
  if (fnName === 'isRange') fnName = 'isInRange'
  if (re) {
    checkitRegexDict[fnName] = 'checkit "' + k + '" regex ' +
      re.toString().replace(/\|/g, '&#124;');
    testFn = function (s) { return re.test(s); }
  } else {
    checkitOtherDict[fnName] =
      'checkit validator [' + k + '](https://github.com/tgriesser/checkit#available-validators)'
    testFn = validatorInstance[k].bind(validatorInstance);
  }

  checkitMixins[fnName] = testFn;
});

// Add check methods for all of lodash "...Case" conversion methods.
Object.keys(lodash).forEach(function (ldKey) {
  if (/Case$/.test(ldKey)) {
    var testFn = function (s) {
      if(typeof s !== 'string') return false
      return s === lodash[ldKey](s);
    };
    var fnName = 'is' + lodash.upperFirst(ldKey)
    lodashCaseDict[fnName] = 'lodash [' + ldKey + '](https://lodash.com/docs/#' + ldKey +')';
    checkitMixins[fnName] = testFn;
  }
});

Object.keys(checkitMixins).forEach(function (ciKey) {
  var notName = ciKey.replace(/^is/, 'isNot');
  if (! lodash[notName] && !checkitMixins[notName]) {
    checkitMixins[notName] = function () {
      return ! checkitMixins[ciKey].apply(null, arguments);
    };
    notDict[notName] = 'not "' + ciKey + '"';
  }
});


module.exports = lodash.runInContext().mixin(checkitMixins);
