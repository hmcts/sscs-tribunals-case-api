#!/usr/bin/env bash

branchName=$1

#Checkout specific branch of sscs-task-configuration
git clone https://github.com/hmcts/sscs-task-configuration.git

if [ ! -d "./sscs-task-configuration" ]; then
  exit 1
fi

echo "Switch to ${branchName} branch on sscs-task-configuration"
cd sscs-task-configuration
git checkout ${branchName}
cd ..

#Copy dmn files to camunda folder
if [ ! -d "./camunda" ]; then
  mkdir camunda
fi

cp -r ./sscs-task-configuration/src/main/resources/*.dmn ./camunda
rm -rf ./sscs-task-configuration