function isPureObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function isObject(value) {
  return value !== null && typeof value === 'object';
}

function isPrimitive(value) {
  return typeof value !== 'object';
}

function getType(value) {
  const type = typeof value;
  if (type === 'object') {
    if (Array.isArray(value)) return 'array';
    if (value === null) return 'null';
  }
  return type;
}

module.exports = {
  isPureObject,
  isObject,
  getType,
  isPrimitive
};