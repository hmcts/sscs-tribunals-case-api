#!/usr/bin/env bash
set -eu

BASEDIR=$(dirname "$0")


IDAM_DATA_STORE_SYSTEM_USER_USERNAME=$(az keyvault secret show --vault-name ccd-aat -o tsv --query value --name idam-data-store-system-user-username)
IDAM_DATA_STORE_SYSTEM_USER_PASSWORD=$(az keyvault secret show --vault-name ccd-aat -o tsv --query value --name idam-data-store-system-user-password)

IDAM_SSCS_SYSTEM_USER_USERNAME=$(az keyvault secret show --vault-name sscs-aat -o tsv --query value --name idam-sscs-systemupdate-user)
IDAM_SSCS_SYSTEM_USER_PASSWORD=$(az keyvault secret show --vault-name sscs-aat -o tsv --query value --name idam-sscs-systemupdate-password)

export IDAM_API_BASE_URL=https://idam-api.aat.platform.hmcts.net
export CCD_API_GATEWAY_IDAM_CLIENT_SECRET=$(az keyvault secret show --vault-name ccd-aat -o tsv --query value --name ccd-api-gateway-oauth2-client-secret)

IDAM_TOKEN=$(${BASEDIR}/utils/idam-lease-user-token.sh $IDAM_SSCS_SYSTEM_USER_USERNAME $IDAM_SSCS_SYSTEM_USER_PASSWORD)
S2S_TOKEN=$(${BASEDIR}/utils/s2s-token.sh "sscs")

IDAM_DEFAULT_USER_PASSWORD=$(az keyvault secret show --vault-name sscs-aat -o tsv --query value --name test-e2e-superuser-password)

JSON_FILE="${BASEDIR}/idam-aat-users.json"
URL="https://idam-testing-support-api.aat.platform.hmcts.net/test/idam/users"

jq -c --arg pwd "$IDAM_DEFAULT_USER_PASSWORD" '.[] | .password = $pwd' "$JSON_FILE" | while IFS= read -r payload; do
  curl --silent --show-error  "${URL}" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${IDAM_TOKEN}" \
  -H "ServiceAuthorization: ${S2S_TOKEN}" \
  -d "${payload}"
done

