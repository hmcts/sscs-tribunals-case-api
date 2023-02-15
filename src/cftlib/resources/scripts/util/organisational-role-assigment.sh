#!/bin/bash
## Usage: ./organisational-role-assignment.sh [username] [password] [role_classification] [role_name] [role_attributes] [microservice_name]
##
## Options:
##    - username: Email for user. Default to `ccd-import@fake.hmcts.net`.
##    - password: Password for user. Default to `London01`.
##    - role_classification: Role assignment classification. Default to `PUBLIC`.
##    - role_name: Name of the role for role-assignment. Default to `tribunal-caseworker`.
##    - role_attributes: Role attributes to add to role assignment. Default to `jurisdiction: "IA"`.
##    - microservice_name: Name of the microservice to obtain S2S token. Default to `ccd_gw`.
##

USERNAME=${1:-local.test@example.com}
PASSWORD=${2:-password}
ROLE_CLASSIFICATION="${3:-PUBLIC}"
ROLE_NAME="${4:-"tribunal-caseworker"}"
ROLE_ATTRIBUTES=${5:-"IA"}
MICROSERVICE="${6:-ccd_gw}"
ROLE_ASSIGNMENT_URL="${ROLE_ASSIGNMENT_URL:-http://localhost:4096}"

BASEDIR=$(dirname "$0")

echo "USERNAME = ${USERNAME}"
echo "PASSWORD = ${PASSWORD}"
USER_TOKEN=$($BASEDIR/idam-user-token.sh $USERNAME $PASSWORD)
echo "USER_TOKEN = ${USER_TOKEN}"
USER_ID=$($BASEDIR/idam-user-id.sh $USER_TOKEN)
echo "USER_ID = ${USER_ID}"
SERVICE_TOKEN=$($BASEDIR/idam-service-token.sh $MICROSERVICE)
echo "SERVICE_TOKEN = ${SERVICE_TOKEN}"

curl --silent --show-error -X POST "${ROLE_ASSIGNMENT_URL}/am/role-assignments" \
  -H "accept: application/vnd.uk.gov.hmcts.role-assignment-service.create-assignments+json;charset=UTF-8;version=1.0" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "ServiceAuthorization: Bearer ${SERVICE_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{ "roleRequest": {
          "assignerId": "'"${USER_ID}"'",
          "process": "staff-organisational-role-mapping",
          "reference": "'"${USER_ID}/${ROLE_NAME}"'",
          "replaceExisting": false
        },
        "requestedRoles": [
          {
            "actorIdType": "IDAM",
            "actorId": "'"${USER_ID}"'",
            "roleType": "ORGANISATION",
            "roleName": "'"${ROLE_NAME}"'",
            "classification": "'"${ROLE_CLASSIFICATION}"'",
            "grantType": "STANDARD",
            "roleCategory": "LEGAL_OPERATIONS",
            "readOnly": false,
            "attributes":
            {
              "jurisdiction": "'"${ROLE_ATTRIBUTES}"'"
            }
          }
        ]
      }'