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
IDAM_TEST_USER_EMAILS=$(az keyvault secret show --vault-name sscs-aat -o tsv --query value --name test-aat-user-emails)

JSON_FILE="${BASEDIR}/idam-aat-users.json"
URL="https://idam-testing-support-api.aat.platform.hmcts.net/test/idam/users"

IFS=',' read -r -a email_array <<< "$IDAM_TEST_USER_EMAILS"

json_entries=()
while IFS= read -r line; do
  json_entries+=("$line")
done < <(jq -c '.[]' "$JSON_FILE")

for i in "${!json_entries[@]}"; do
  payload="${json_entries[$i]}"
  email="${email_array[$i]}"

  final_payload=$(echo "$payload" | jq -c --arg pwd "$IDAM_DEFAULT_USER_PASSWORD" --arg email "$email" '.password = $pwd | .user.email = $email')

  curl --silent --show-error  "${URL}" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${IDAM_TOKEN}" \
  -H "ServiceAuthorization: ${S2S_TOKEN}" \
  -d "${final_payload}"
done

