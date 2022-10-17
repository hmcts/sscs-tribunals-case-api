#!/bin/bash

declare -A secrets

KEY_VAULT_NAME="sscs-aat"

#Key-value array of secrets. key=azure secret name, value=desired environment variable
secrets=(["docmosis-endpoint"]="DOCMOSIS_TEMPLATES_ENDPOINT_AUTH"
["docmosis-api-key"]="DOCMOSIS_ACCESS_KEY")

echo "Fetching secrets from $KEY_VAULT_NAME..."

#Loop through secrets, trimming leading and trailing quotes.
for i in "${!secrets[@]}"
do
  export "${secrets[$i]}"="$(az keyvault secret show --name "$i" --vault-name $KEY_VAULT_NAME --query "value" | sed -e 's/^"//' -e 's/"$//')"
done

echo "Secret fetching complete"

./gradlew bootWithCCD
