const chai = require('chai');
const expect = chai.expect;
const sinon = require('sinon');
const assert = require('assert');
const sinonChai = require('sinon-chai');
chai.should();
chai.use(sinonChai);

module.exports = {
    expect,
    sinon,
    assert
};
