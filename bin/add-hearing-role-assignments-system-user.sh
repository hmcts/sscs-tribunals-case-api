#!/usr/bin/env bash
set -eu

export IDAM_API_BASE_URL=https://idam-api.aat.platform.hmcts.net

BASEDIR=$(dirname "$0")
S2S_TOKEN=$(${BASEDIR}/utils/s2s-token.sh "am_org_role_mapping_service")
SSCS_SYSTEM_USER_TOKEN=$(${BASEDIR}/utils/idam-lease-user-token.sh $IDAM_SSCS_SYSTEMUPDATE_USER $IDAM_SSCS_SYSTEMUPDATE_PASSWORD)
SYSTEM_USER_ID=$(curl --silent --show-error -X GET "${IDAM_API_BASE_URL}/details" -H "accept: application/json" -H "authorization: Bearer ${SSCS_SYSTEM_USER_TOKEN}" | jq -r .id)

echo -e "\nCreating role assignment: \n User: ${SYSTEM_USER_ID}"

curl --silent --show-error -X POST "https://am-role-assignment-service-sscs-tribunals-api-pr-${CHANGE_ID}.preview.platform.hmcts.net/am/role-assignments" \
  -H "accept: application/vnd.uk.gov.hmcts.role-assignment-service.create-assignments+json;charset=UTF-8;version=1.0" \
  -H "Authorization: Bearer ${SSCS_SYSTEM_USER_TOKEN}" \
  -H "ServiceAuthorization: Bearer ${S2S_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{ "roleRequest": {
            "assignerId": "41033a79-b9c1-4a36-b0ff-113451f736ba",
            "process": "sscs-system-users",
            "reference": "sscs-hearings-system-user",
            "replaceExisting": true
          },
          "requestedRoles": [
            {
              "actorId": "'"${SYSTEM_USER_ID}"'",
              "roleType": "ORGANISATION",
              "classification": "PUBLIC",
              "roleName": "hearing-manager",
              "roleCategory": "SYSTEM",
              "grantType": "STANDARD",
              "attributes": {
                "jurisdiction": "SSCS",
                "caseType": "Benefit"
              },
              "actorIdType": "IDAM"
            },
            {
              "actorId": "'"${SYSTEM_USER_ID}"'",
              "roleType": "ORGANISATION",
              "classification": "PUBLIC",
              "roleName": "hearing-viewer",
              "roleCategory": "SYSTEM",
              "grantType": "STANDARD",
              "attributes": {
                "jurisdiction": "SSCS",
                "caseType": "Benefit"
              },
              "actorIdType": "IDAM"
            }
          ]
      }'
