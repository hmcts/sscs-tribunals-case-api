#!/bin/bash

JSON_FILE=${1:-"./definitions/benefit/sheets/CaseEvent/CaseEvent.json"}

if [ ! -f "$JSON_FILE" ]; then
    echo "❌ Error: JSON file not found at $JSON_FILE"
    exit 1
fi

# Change top level Publish attribute
jq '.[].Publish = "N"' "$JSON_FILE" > temp.json && mv temp.json "$JSON_FILE"

# Check it worked
if [ $? -eq 0 ]; then
    echo "✅ Successfully updated 'Publish' to 'Y' in $JSON_FILE."
else
    echo "❌ Error during JSON update. The file was not modified."
    exit 1
fi