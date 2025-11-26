#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <type>"
  exit 1
fi

TYPE="$1"
SRC_DIR="$(pwd)/benefit"
DEST_DIR="$(pwd)/${TYPE}"

# Ensure the source directory exists
if [[ ! -d "$SRC_DIR" ]]; then
  echo "Source directory 'benefit' does not exist in the current path."
  exit 1
fi

# Copy the directory
cp -R "$SRC_DIR" "$DEST_DIR"

# Replace 'Benefit' with the given type in JSON files only
while IFS= read -r -d '' FILE; do
  sed -i '' "s/Benefit/${TYPE}/g" "$FILE"
done < <(find "$DEST_DIR" -type f -name "*.json" -print0)

# Replace the entire Name value inside sheets/CaseType/CaseType.json
CT_FILE="${DEST_DIR}/sheets/CaseType/CaseType.json"

if [[ -f "$CT_FILE" ]]; then
  # Replace the Name field’s value entirely with the type
  sed -i '' "s/\"Name\": \".*\"/\"Name\": \"${TYPE}\"/" "$CT_FILE"
fi

echo "All updates complete under '${DEST_DIR}'."
