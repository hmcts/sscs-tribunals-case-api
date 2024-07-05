#!/bin/bash

# Set environment variable from Azure secret vault
# Parameters: <Environment variable name> <Vault Name> <Secret Name>
loadSecret () {
  export "$1"="$(az keyvault secret show --name "$3" --vault-name $2 --query "value" | sed -e 's/^"//' -e 's/"$//')"
}

echo "Fetching secrets..."

loadSecret "DOCMOSIS_TEMPLATES_ENDPOINT_AUTH" "dg-docassembly-aat" "docmosis-templates-auth"
loadSecret "DOCMOSIS_ACCESS_KEY" "dg-docassembly-aat" "docmosis-access-key"
loadSecret "AZURE_SERVICE_BUS_CONNECTION_STRING" "sscs-aat" "sscs-servicebus-connection-string-tf"
loadSecret "LAUNCH_DARKLY_SDK_KEY" "wa-aat" "ld-secret"

echo "Secret fetching complete"

az acr login --name hmctspublic --subscription 8999dec3-0104-4a27-94ee-6588559729d1

echo "Switch to definitions folder"
cd definitions
git submodule update --init
git checkout master
git pull
echo "Switch to parent folder"
cd ../

HOST_ENTRY="127.0.0.1    rse-idam-simulator"

if grep -q "$HOST_ENTRY" /etc/hosts; then
  echo "Entry already exists in /etc/hosts"
else
  sudo bash -c "echo '$HOST_ENTRY' >> /etc/hosts"
fi

echo "pull rse-idam-simulator..."
docker pull hmctspublic.azurecr.io/hmcts/rse/rse-idam-simulator:latest

./gradlew bootWithCCD