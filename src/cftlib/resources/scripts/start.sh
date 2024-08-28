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
loadSecret "DM_STORE_STORAGEACCOUNT_PRIMARY_CONNECTION_STRING" "sscs-aat" "local-dm-store-storage-acc-prime-conn-string"
loadSecret "LAUNCH_DARKLY_SDK_KEY" "wa-aat" "ld-secret"

echo "Secret fetching complete"

./gradlew bootWithCCD
