'use strict'

const { expect, sinon } = require('../chai-sinon');
const express = require('../../log/express');
const http = require('http');
const request = require('supertest');

describe('Express.js application logging', () => {
  let myLogger, logger;

  beforeEach(() => {
    myLogger = require('../../log/Logger');
  });

  describe('unit', () => {
    let middleware;

    beforeEach(() => {
      logger = {
        info: sinon.spy(),
        warn: sinon.spy(),
        error: sinon.spy(),
      };
      logger.info.namey = "info";
      logger.warn.namey = "warn";
      logger.error.namey = "error";
      middleware = express.accessLogger({logger: logger});
    });

    describe('logging', () => {
      it('should log a successful request on /', (done) => {
        request(createServer(middleware))
          .get('/')
          .expect(200, () => {
            expect(logger.info).to.have.been.calledWith(sinon.match({
              responseCode: 200,
              message: '"GET / HTTP/1.1" 200',
            }));
            done()
          });
      });

      it('should log a successful request on /foo', (done) => {
        request(createServer(middleware))
          .get('/foo')
          .expect(200, () => {
            expect(logger.info).to.have.been.calledWith(sinon.match({
              responseCode: 200,
              message: '"GET /foo HTTP/1.1" 200',
            }));
            done()
          });
      });

      it('should log a 400 on /', (done) => {
        request(createServer(middleware, {statusCode: 400}))
          .get('/')
          .expect(400, () => {
            expect(logger.warn).to.have.been.calledWith(sinon.match({
              responseCode: 400,
              message: '"GET / HTTP/1.1" 400',
            }));
            done();
          });
      });

      it('should log a 500 on /', (done) => {
        request(createServer(middleware, {statusCode: 500}))
          .get('/')
          .expect(500, () => {
            expect(logger.error).to.have.been.calledWith(sinon.match({
              responseCode: 500,
              message: '"GET / HTTP/1.1" 500',
            }));
            done();
          });
      });
    });

    describe('configuration', () => {
      it('should use user provided formatter', (done) => {
        middleware = express.accessLogger({
          logger: logger,
          formatter: () => {
            return "my format"
          }
        });
        request(createServer(middleware))
          .get('/')
          .expect(200, () => {
            expect(logger.info).to.have.been.calledWith(sinon.match({
              message: "my format"
            }));
            done();
          });
      });

      it('should use user provided log levels', (done) => {
        middleware = express.accessLogger({
          logger: logger,
          level: (logger, req, res) => {
            if (res.statusCode === 200) {
              return logger.error;
            }
          }
        });
        request(createServer(middleware))
          .get('/')
          .expect(200, () => {
            expect(logger.error).to.have.been.called
            expect(logger.info).to.have.not.been.called
            done();
          });
      });
    });

    describe('request tracing headers', () => {
      let req

      beforeEach(() => {
        const headers = { }
        headers[REQUEST_ID_HEADER] = 'test-request-id'
        headers[ORIGIN_REQUEST_ID_HEADER] = 'test-origin-request-id'
        headers[ROOT_REQUEST_ID_HEADER] = 'test-root-request-id'
        req = {
          headers: headers
        }
      })
    })
  });
});

function createServer (middleware, config = { }) {
  return http.createServer(function onRequest (req, res) {
    middleware(req, res, function onNext (err) {
      // allow req, res alterations
      if (config.req && config.req.headers) {
        req.headers = { ...req.headers, ...config.req.headers }
      }
      if (err) {
        res.statusCode = 500
        res.end(err.message)
      } else {
        res.statusCode = config.statusCode || 200;
      }

      res.setHeader('X-Sent', 'true')
      res.end((req.connection && req.connection.remoteAddress) || '-')
    })
  })
}
