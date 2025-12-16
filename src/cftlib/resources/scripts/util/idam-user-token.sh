#!/bin/bash
## Usage: ./idam-user-token.sh [user] [password]
##
## Options:
##    - username: Role assigned to user in generated token. Default to `ccd-import@fake.hmcts.net`.
##    - password: ID assigned to user in generated token. Default to `London01`.
##

USERNAME=${1:-sscs-ctscdmin-nw-liverpool@justice.gov.uk}
PASSWORD=${2:-Testing123}
REDIRECT_URI="https://evidence-sharing-preprod.sscs.reform.hmcts.net"
CLIENT_ID="sscs"
CLIENT_SECRET="naA2#nvAJ$ge^eEzW2"
SCOPE="openid%20profile%20roles%20authorities"
IDAM_API_URL="${IDAM_API_URL:-https://idam-api.aat.platform.hmcts.net}"

curl --silent --show-error \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -X POST "${IDAM_API_URL}/o/token?grant_type=password&redirect_uri=${REDIRECT_URI}&client_id=${CLIENT_ID}&client_secret=${CLIENT_SECRET}&username=${USERNAME}&password=${PASSWORD}&scope=${SCOPE}" -d "" | jq -r .access_token
