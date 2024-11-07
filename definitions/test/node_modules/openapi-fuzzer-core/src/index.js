const Swagger = require('./swagger');

const fuzz = {

  swagger(data) {
    const fuzzes = new Swagger(data);
    fuzzes.fuzz();
    return fuzzes.specs;
  }

};

module.exports = fuzz;