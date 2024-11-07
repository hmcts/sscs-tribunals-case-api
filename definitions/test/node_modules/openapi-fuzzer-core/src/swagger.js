const { klona } = require("klona");

const HTTP_METHODS = ['get', 'post', 'put', 'patch', 'delete'];
const HTTP_METHODS_SET = new Set(HTTP_METHODS);

class Swagger {

  constructor(data) {
    this.data = data;
    this.basePath = data.basePath;
    this.specs = [];
  }

  fuzz() {
    this.fuzzPaths();
    this.fuzzMethods();
    this.fuzzParams();
  }

  fuzzPaths() {
    HTTP_METHODS.forEach(method => {
      this.specs.push({
        name: 'PATH',
        request: {
          method,
          path: '/ROOT/INVALID/PATH'
        },
        expect: {
          status: [404]
        }
      });
    });
    for (const route of Object.keys(this.data.paths)) {
      const path = this.data.paths[route];
      for (const method of Object.keys(path)) {
        if (HTTP_METHODS_SET.has(method)) {
          this.specs.push({
            name: 'PATH',
            request: {
              method,
              path: `${this.basePath}${route}/INVALID/PATH`
            },
            expect: {
              status: [404]
            }
          });
        }
      }
    }
  }

  fuzzMethods() {
    for (const route of Object.keys(this.data.paths)) {
      const path = this.data.paths[route];
      HTTP_METHODS.forEach(method => {
        if (!path[method]) {
          this.specs.push({
            name: 'METHOD',
            request: {
              method,
              path: `${this.basePath}${route}`
            },
            expect: {
              status: [405]
            }
          });
        }
      });
    }
  }

  fuzzParams() {
    for (const route of Object.keys(this.data.paths)) {
      const path = this.data.paths[route];
      for (const method of Object.keys(path)) {
        if (HTTP_METHODS_SET.has(method)) {
          const info = path[method];
          const parameters = info.parameters;
          if (parameters && parameters.length > 0) {
            this.fuzzPathParams(route, method, parameters);
            this.fuzzQueryParams(route, method, parameters);
            this.fuzzBodyParams(route, method, parameters);
          }
        }
      }
    }
  }

  fuzzPathParams(route, method, parameters) {
    const pathParameters = parameters.filter(parameter => parameter.in === 'path');
    const fakePathParams = this.getFakeParams(pathParameters);
    for (const parameter of pathParameters) {
      const values = this.fuzzParameter(parameter);
      for (const value of values) {
        const pathParams = Object.assign({}, fakePathParams);
        pathParams[`${parameter.name}`] = value;
        this.specs.push({
          name: 'PATH PARAMS',
          request: {
            method,
            path: `${this.basePath}${route}`,
            pathParams
          },
          expect: {
            status: [400]
          }
        });
      }
    }
  }

  fuzzQueryParams(route, method, parameters) {
    const pathParameters = parameters.filter(parameter => parameter.in === 'path');
    const pathParams = this.getFakeParams(pathParameters);
    const queryParameters = parameters.filter(parameter => parameter.in === 'query');
    const previousQueryParams = {};
    for (const parameter of queryParameters) {
      const values = this.fuzzParameter(parameter);
      for (const value of values) {
        const queryParams = Object.assign({}, previousQueryParams);
        queryParams[`${parameter.name}`] = value;
        this.specs.push({
          name: 'QUERY PARAMS',
          request: {
            method,
            path: `${this.basePath}${route}`,
            queryParams,
            pathParams
          },
          expect: {
            status: [400]
          }
        });
      }
      previousQueryParams[`${parameter.name}`] = this.fakeParameter(parameter);
    }
  }

  fuzzBodyParams(route, method, parameters) {
    const pathParameters = parameters.filter(parameter => parameter.in === 'path');
    const pathParams = this.getFakeParams(pathParameters);
    const queryParameters = parameters.filter(parameter => parameter.in === 'query');
    const queryParams = this.getFakeParams(queryParameters);
    const bodyParameters = parameters.filter(parameter => parameter.in === 'body');
    for (const parameter of bodyParameters) {
      const schema = parameter.schema;
      if (schema && schema['$ref']) {
        const values = this.fuzzParameter(this.getDefinition(schema['$ref']), true);
        for (const value of values) {
          if (typeof value === 'number' || typeof value === 'boolean') continue;
          this.specs.push({
            name: 'BODY',
            request: {
              method,
              path: `${this.basePath}${route}`,
              queryParams,
              pathParams,
              body: value
            },
            expect: {
              status: [400]
            }
          });
        }
      }
    }
  }

  fuzzParameter(parameter, nonPrimitives) {
    switch (parameter.type) {
      case 'integer':
        return this.fuzzIntegerParam(parameter, nonPrimitives);
      case 'string':
        return this.fuzzStringParam(parameter, nonPrimitives);
      case 'boolean':
        return this.fuzzBooleanParam(parameter, nonPrimitives);
      case 'object':
        return this.fuzzObjectParam(parameter, nonPrimitives);
      case 'array':
        return this.fuzzArrayParam(parameter, nonPrimitives);
      default:
        if (parameter['$ref']) {
          return this.fuzzParameter(this.getDefinition(parameter['$ref']), nonPrimitives);
        }
        return [];
    }
  }

  fuzzIntegerParam(parameter, nonPrimitives) {
    const values = ['STRING', true];
    if (typeof parameter.minimum === 'number') {
      values.push(parameter.minimum - 1);
    }
    if (typeof parameter.maximum === 'number') {
      values.push(parameter.maximum + 1);
    }
    if (nonPrimitives) {
      return values.concat([{}, null, []]);
    }
    return values;
  }

  fuzzStringParam(parameter, nonPrimitives) {
    let values = [];
    if (nonPrimitives) {
      values = values.concat([10, true, {}, null, []]);
    }
    if (parameter.minLength > 0) {
      values.push('');
    }
    return values;
  }

  fuzzBooleanParam(parameter, nonPrimitives) {
    if (nonPrimitives) {
      return ['STRING', 10, {}, null, []];
    }
    return ['STRING', 10];
  }

  fuzzObjectParam(parameter, nonPrimitives) {
    const values = ['', 'STRING', 10, true, null, []];
    const { required, properties } = parameter;
    if (required && required.length > 0) {
      values.push({});
      for (const req of required) {
        const otherRequiredParams = required.filter(_req => _req !== req);
        const value = {};
        for (const otherReqParam of otherRequiredParams) {
          value[otherReqParam] = this.fakeParameter(properties[otherReqParam]);
        }
        values.push(value);
      }
    }
    const goldenCopy = this.fakeParameter(parameter);
    if (properties) {
      for (const property of Object.keys(properties)) {
        const fuzzValues = this.fuzzParameter(properties[property], nonPrimitives);
        for (const fuzzValue of fuzzValues) {
          const fake = klona(goldenCopy);
          fake[property] = fuzzValue;
          values.push(fake);
        }
      }
    }
    return values;
  }

  fuzzArrayParam(parameter, nonPrimitives) {
    const values = ['', 'STRING', 10, true, null, {}];
    const fzs = this.fuzzParameter(parameter.items, nonPrimitives);
    fzs.forEach(fuzz => values.push([fuzz]));
    return values;
  }

  fakeParameter(parameter) {
    let fake;
    const { example, properties } = parameter;
    switch (parameter.type) {
      case 'integer':
        return 10;
      case 'string':
        if (parameter.example) return parameter.example;
        if (parameter.enum) return parameter.enum[0];
        if (parameter.name) return parameter.name;
        return 'STRING';
      case 'boolean':
        return true;
      case 'object':
        fake = {};
        if (properties) {
          for (const property of Object.keys(properties)) {
            fake[property] = this.fakeParameter(properties[property]);
          }
        } else if (example) {
          fake = example;
        }
        return fake;
      case 'array':
        fake = [];
        fake.push(this.fakeParameter(parameter.items));
        return fake;
      default:
        if (parameter['$ref']) {
          return this.fakeParameter(this.getDefinition(parameter['$ref']));
        }
        return '';
    }
  }

  getFakeParams(parameters) {
    const pathParams = {};
    for (const parameter of parameters) {
      pathParams[`${parameter.name}`] = this.fakeParameter(parameter);
    }
    return pathParams;
  }

  getDefinition(ref) {
    const definition = ref.replace('#/definitions/', '');
    return this.data.definitions[definition];
  }

}

module.exports = Swagger;