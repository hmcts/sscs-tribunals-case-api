#!/usr/bin/env bash
set -eu

# LOCALLY:
CHANGE_ID=4357
IDAM_DATA_STORE_SYSTEM_USER_USERNAME=$(az keyvault secret show --vault-name ccd-aat -o tsv --query value --name idam-data-store-system-user-username)
IDAM_DATA_STORE_SYSTEM_USER_PASSWORD=$(az keyvault secret show --vault-name ccd-aat -o tsv --query value --name idam-data-store-system-user-password)
export CCD_API_GATEWAY_IDAM_CLIENT_SECRET=$(az keyvault secret show --vault-name ccd-aat -o tsv --query value --name ccd-api-gateway-oauth2-client-secret)

BASEDIR=$(dirname "$0")
S2S_TOKEN=$(${BASEDIR}/utils/s2s-token.sh "am_org_role_mapping_service")

export IDAM_API_BASE_URL=https://idam-api.aat.platform.hmcts.net

IDAM_TOKEN=$(${BASEDIR}/utils/idam-lease-user-token.sh $IDAM_DATA_STORE_SYSTEM_USER_USERNAME $IDAM_DATA_STORE_SYSTEM_USER_PASSWORD)
PAYLOAD='{"userIds": ["41033a79-b9c1-4a36-b0ff-113451f736ba", "a9ab7f4b-7e0c-49d4-8ed3-75b54d421cdc", "22cb52eb-9490-42ce-837b-58b81702855a"]}'
URL="https://orm-sscs-tribunals-api-pr-${CHANGE_ID}.preview.platform.hmcts.net/am/testing-support/createOrgMapping?userType=CASEWORKER"

curl --silent --show-error --fail "${URL}" \
-H 'Content-Type: application/json' \
-H "Authorization: Bearer ${IDAM_TOKEN}" \
-H "ServiceAuthorization: ${S2S_TOKEN}" \
-d "${PAYLOAD}"
