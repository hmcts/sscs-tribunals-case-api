const { isObject } = require('./helpers');

function getValueFromMatcher(data) {
  switch (data.pactum_type) {
    case 'ONE_OF':
      return data.value[0];
    case 'ARRAY_LIKE':
      if (data.items && data.items.length > 0) {
        data.value = data.items;
      } else if (Array.isArray(data.value)) {
        for (let i = 0; i < data.value.length; i++) {
          data.value[i] = getValue(data.value[i]);
        }
      }
      return data.value;
    default:
      return data.value;
  }
}

function getValue(data) {
  if (Array.isArray(data)) {
    for (let i = 0; i < data.length; i++) {
      data[i] = getValue(data[i]);
    }
  } else if (isObject(data)) {
    if (data.pactum_type) {
      data = getValueFromMatcher(data);
    }
    if (isObject(data)) {
      for (const prop in data) {
        data[prop] = getValue(data[prop]);
      }
    }
  }
  return data;
}

module.exports = {
  getValue
};