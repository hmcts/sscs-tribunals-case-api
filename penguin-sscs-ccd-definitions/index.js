const express = require('express');
const healthcheck = require('@hmcts/nodejs-healthcheck');

const app = express();
const payload = {message: 'Im all right !'};
const config = require('@hmcts/properties-volume').addTo(require('config'));

const port = config.get('server.port');

healthcheck.addTo(app,
    {
        checks: {
            secretsCheck: healthcheck.raw(() => {
                return healthcheck.up();
            })
        },
        buildInfo: {'chart-testing': 'nodejs-chart test'}
    });

app.get('/', (req, res) => {
    return res.send(payload);
})
    .listen(port, () => {
        // eslint-disable-next-line no-console
        return console.log(`chart-nodeJs test app listening on http://0.0.0.0:${port}`);
    });