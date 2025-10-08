#!/usr/bin/env bash
set -eu

# LOCALLY:
#CHANGE_ID=4357
#IDAM_DATA_STORE_SYSTEM_USER_USERNAME=$(az keyvault secret show --vault-name ccd-aat -o tsv --query value --name idam-data-store-system-user-username)
#IDAM_DATA_STORE_SYSTEM_USER_PASSWORD=$(az keyvault secret show --vault-name ccd-aat -o tsv --query value --name idam-data-store-system-user-password)
#export CCD_API_GATEWAY_IDAM_CLIENT_SECRET=$(az keyvault secret show --vault-name ccd-aat -o tsv --query value --name ccd-api-gateway-oauth2-client-secret)

BASEDIR=$(dirname "$0")
S2S_TOKEN=$(${BASEDIR}/utils/s2s-token.sh "am_org_role_mapping_service")

export IDAM_API_BASE_URL=https://idam-api.aat.platform.hmcts.net

IDAM_TOKEN=$(${BASEDIR}/utils/idam-lease-user-token.sh $IDAM_DATA_STORE_SYSTEM_USER_USERNAME $IDAM_DATA_STORE_SYSTEM_USER_PASSWORD)

function send_curl_request() {
  local json_file=$1
  local user_type=$2

  if [[ ! -f "${json_file}" ]]; then
    echo "File not found: ${json_file}"
    return 1
  fi

  local payload=$(cat "${json_file}")
  local url="https://orm-sscs-tribunals-api-pr-${CHANGE_ID}.preview.platform.hmcts.net/am/testing-support/createOrgMapping?userType=${user_type}"

  curl --silent --show-error --fail "${url}" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${IDAM_TOKEN}" \
  -H "ServiceAuthorization: ${S2S_TOKEN}" \
  -d "${payload}"
}

send_curl_request "${BASEDIR}/staff-idam-ids.json" "CASEWORKER"
send_curl_request "${BASEDIR}/sscs-judicial-idam-ids.json" "JUDICIAL"
