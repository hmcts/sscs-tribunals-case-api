#!/usr/bin/env bash
echo ${TEST_URL}
zap-api-scan.py -t ${TEST_URL}/v2/api-docs -f openapi -u ${SecurityRules} -P 1001 -l FAIL
cat zap.out
zap-cli --zap-url http://0.0.0.0 -p 1001 report -o /zap/api-report.html -f html
cp /zap/api-report.html functional-output/
zap-cli -p 1001 alerts -l High
ruby bin/glue -t zap --zap-host http://0.0.0.0 --zap-port 1001 --zap-passive-mode ${TEST_URL}/v2/api-docs -z 0 --finding-file glue.json