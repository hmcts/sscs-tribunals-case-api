#!/usr/bin/env bash

set -eu

dir=$(dirname ${0})

jq -c '(.[])' ${dir}/../am-role-assignments.json | while read user; do
  email=$(jq -r '.email' <<< $user)
  token=$(${dir}/util/idam-user-token.sh $email)
  idamId=$(${dir}/util/idam-user-id.sh $token)

  override=$(jq -r '.overrideAll' <<< $user)
  if [ $override == 'true' ]; then
    echo "Removing all existing role assignments for user ${email}"
    psql -h localhost -p ${DB_EXTERNAL_PORT} -d am -U postgres -c "DELETE FROM role_assignment WHERE actor_id = '${idamId}'" -q
  fi

  jq -c '(.roleAssignments[])' <<< $user | while read assignment; do
    roleType=$(jq -r '.roleType' <<< $assignment)
    roleName=$(jq -r '.roleName' <<< $assignment)
    grantType=$(jq -r '.grantType' <<< $assignment)
    roleCategory=$(jq -r '.roleCategory' <<< $assignment)
    classification=$(jq -r '.classification' <<< $assignment)
    readOnly=$(jq -r '.readOnly' <<< $assignment)
    attributes=$(jq -r '.attributes | tostring' <<< $assignment)

    authorisations=$(jq -r 'if .authorisations | length > 0 then "'"'"'{" + (.authorisations | join(",")) + "}'"'"'" else null end' <<< $assignment)

    echo "Creating '${roleName}' assignment of type '${roleType}' for user ${email}"
    ${dir}/util/am-add-role-assignment.sh $idamId $roleType $roleName $classification $grantType $roleCategory $readOnly $attributes $authorisations
  done
  echo
done