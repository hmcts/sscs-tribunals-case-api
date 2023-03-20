#!/bin/bash
## Usage: ./camunda-deployment
##
## Options:
##    - SERVICE_TOKEN: which is generated with the idam-service-token.
##
## deploys bpmn/dmn to camunda.

WA_BPMNS_DMNS_PATH="../wa-standalone-task-bpmn"
SSCS_DMNS_PATH="../sscs-task-configuration"
SERVICE_TOKEN="$(sh ./idam-service-token.sh "wa_camunda_pipeline_upload")"

echo "Uploading Camunda BPMs and DMNs..."

if [[ -z "${WA_BPMNS_DMNS_PATH}" ]]; then
  echo ""
  echo "Environment variable WA_BPMNS_DMNS_PATH was not set skipping deployment."
else
  echo "Deploying WA Standalone Task BPMN and DMNs..."
  echo "SERVICE_TOKEN=${SERVICE_TOKEN}"
  $WA_BPMNS_DMNS_PATH/camunda-deployment.sh $SERVICE_TOKEN
fi

if [[ -z "${SSCS_DMNS_PATH}" ]]; then
  echo ""
  echo "Environment variable SSCS_DMNS_PATH was not set skipping deployment."
else
  echo "Deploying SSCS Task Configuration BPMN and DMNs..."
  echo "SERVICE_TOKEN=${SERVICE_TOKEN}"
  $SSCS_DMNS_PATH/camunda-deployment.sh $SERVICE_TOKEN
fi
