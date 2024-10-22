#!/usr/bin/env bash

USERNAME=$(whoami)

cat <<EOF > .env.dev
PREVIEW_BRANCH_WITH_LOCAL_CCD=true
TEST_E2E_URL_WEB=https://xui-sscs-tribunals-api-${USERNAME}-pr-1.preview.platform.hmcts.net
TEST_E2E_API_URI=https://sscs-tribunals-api-${USERNAME}-pr-1.preview.platform.hmcts.net
CCD_API_URL=https://ccd-data-store-api-sscs-tribunals-api-${USERNAME}-pr-1.preview.platform.hmcts.net
CORE_CASE_DATA_API_URL=https://ccd-data-store-api-sscs-tribunals-api-${USERNAME}-pr-1.preview.platform.hmcts.net
CCD_GW_API=http://sscs-tribunals-api-${USERNAME}-pr-1-ccd-api-gw
CASE_DOCUMENT_AM_URL=https://ccd-case-document-am-api-sscs-tribunals-api-${USERNAME}-pr-1.preview.platform.hmcts.net
ENVIRONMENT=pr
EOF

npx @hmcts/dev-env --env .env.dev

rm .env.dev

export EM_CCD_ORCHESTRATOR_URL=https://em-ccdorc-sscs-tribunals-api-${USERNAME}-pr-1.preview.platform.hmcts.net
export TRIBUNALS_API_URL=${TEST_E2E_API_URI}

./bin/create-xlsx.sh benefit tag pr
./bin/create-xlsx.sh bulkscan tag pr

npx @hmcts/dev-env --import-ccd

rm befta_recent_executions_info_PREVIEW.json
