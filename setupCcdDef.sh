#!/usr/bin/env bash
echo "Setting up ccd definition parameters...."

# move these env variable from values.ccd.preview.template and populate dynamically before setting into the environment
#
# CCD_DEF_FIXED_LIST_USERS,
# CCD_DEF_PIP_DECISION_NOTICE_QUESTIONS,
# CCD_DEF_ESA_DECISION_NOTICE_QUESTIONS,
# CCD_DEF_UC_DECISION_NOTICE_QUESTIONS,
export CCD_DEF_LANGUAGES='{"LiveFrom": "01/01/2018", "ID": "FL_languages", "ListElementCode": "zulu", "ListElement": "Zulu"},{"LiveFrom": "01/01/2018", "ID": "FL_languages", "ListElementCode": "bajuni", "ListElement": "Bajuni"}'
