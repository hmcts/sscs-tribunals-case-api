#!/bin/bash

echo "-> Performing Azure HMCTS Private Login"
TOKEN=$(az acr login --name hmctsprivate --subscription DCD-CNP-PROD --expose-token | jq --raw-output '.accessToken')

if docker login hmctsprivate.azurecr.io --username=00000000-0000-0000-0000-000000000000 --password=$TOKEN > /dev/null ; then
  echo "✅  Logged in successfully"
else
  echo "❌  Something went wrong when performing the log in"
fi
