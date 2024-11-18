#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: $0 <CHANGE_ID>"
    exit 1
fi

CHANGE_ID=$1

if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    find ./definitions/benefit/sheets/ -type f -exec sed -i '' "s/Benefit/Benefit-$CHANGE_ID/g" {} +
    sed -i '' "s/-e \"CCD_DEF_ENV=[^\"]*\"/-e \"CCD_DEF_ENV=${CHANGE_ID}\"/" ./bin/create-xlsx.sh
else
    # Linux and other Unix-like systems
    find ./definitions/benefit/sheets/ -type f -exec sed -i "s/Benefit/Benefit-$CHANGE_ID/g" {} +
    sed -i "s/-e \"CCD_DEF_ENV=[^\"]*\"/-e \"CCD_DEF_ENV=${CHANGE_ID}\"/" ./bin/create-xlsx.sh
fi

./bin/create-xlsx.sh benefit dev pr

git checkout definitions/benefit/sheets bin
