#!/usr/bin/env bash

branchName=$1

#Checkout specific branch of wa-standalone-task-bpmn
git clone https://github.com/hmcts/wa-standalone-task-bpmn.git
cd wa-standalone-task-bpmn

echo "Switch to ${branchName} branch on wa-standalone-task-bpmn"
git checkout ${branchName}
cd ..

#Copy bpmn files to camunda folder
if [ ! -d "./camunda" ]; then
  mkdir camunda
fi

cp -r ./wa-standalone-task-bpmn/src/main/resources/ ./camunda
rm -rf ./wa-standalone-task-bpmn
