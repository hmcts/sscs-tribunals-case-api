#!/usr/bin/env bash

set -eu
workspace=${1}
tenant_id=${2}
product=${3}

s2sSecret=${IDAM_S2S_AUTH_TOTP_SECRET:-AABBCCDDEEFFGGHH}

serviceToken=$($(realpath $workspace)/bin/utils/idam-lease-service-token.sh sscs \
  $(docker run --rm toolbelt/oathtool --totp -b ${s2sSecret}))

camundaFilepath="$(realpath $workspace)/camunda"
if [ ! -d ${camundaFilepath} ]; then
  echo "Directory with camunda definition files is missing: ${camundaFilepath}";
fi

# Load BPMN files
for file in $(find ${camundaFilepath} -name '*.bpmn')
do
  uploadResponse=$(curl --insecure -v --silent -w "\n%{http_code}" --show-error -X POST \
    ${CAMUNDA_BASE_URL:-http://localhost:9404}/engine-rest/deployment/create \
    -H "Accept: application/json" \
    -H "ServiceAuthorization: Bearer ${serviceToken}" \
    -F "deployment-name=$(basename ${file})" \
    -F "deploy-changed-only=true" \
    -F "file=@${camundaFilepath}/$(basename ${file})")

  upload_http_code=$(echo "$uploadResponse" | tail -n1)
  upload_response_content=$(echo "$uploadResponse" | sed '$d')

  if [[ "${upload_http_code}" == '200' ]]; then
    echo "$(basename ${file}) diagram uploaded successfully (${upload_response_content})";
    continue;
  fi

  echo "$(basename ${file}) upload failed with http code ${upload_http_code} and response (${upload_response_content})"
  continue;
done

# Load DMN files
for file in $(find ${camundaFilepath} -name '*.dmn')
do
  uploadResponse=$(curl --insecure -v --silent -w "\n%{http_code}" --show-error -X POST \
    ${CAMUNDA_BASE_URL:-http://localhost:9404}/engine-rest/deployment/create \
    -H "Accept: application/json" \
    -H "ServiceAuthorization: Bearer ${serviceToken}" \
    -F "deployment-name=$(basename ${file})" \
    -F "deploy-changed-only=true" \
    -F "deployment-source=$product" \
    ${tenant_id:+'-F' "tenant-id=$tenant_id"} \
    -F "file=@${camundaFilepath}/$(basename ${file})")

  upload_http_code=$(echo "$uploadResponse" | tail -n1)
  upload_response_content=$(echo "$uploadResponse" | sed '$d')

  if [[ "${upload_http_code}" == '200' ]]; then
    echo "$(basename ${file}) diagram uploaded successfully (${upload_response_content})";
    continue;
  fi

  echo "$(basename ${file}) upload failed with http code ${upload_http_code} and response (${upload_response_content})";
  continue;
done
