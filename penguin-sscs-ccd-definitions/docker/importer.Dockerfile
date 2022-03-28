# ---- Base image - order important ----
FROM hmcts/ccd-definition-processor:latest as base

# ----        Runtime image         ----
FROM hmcts/ccd-definition-importer:latest as runtime

# ---- To build non prod version image, pass --build-arg exclude=prod
ARG exclude=nonprod
ENV EXCLUSION=*-$exclude.json,*-shuttered.json

RUN apk add --no-cache curl jq zip unzip git
COPY --from=base . .
COPY ./data /data
COPY ./data/ccd-template.xlsx /opt/ccd-definition-processor/data

CMD cd /opt/ccd-definition-processor && yarn json2xlsx -D /data/sheets -e $EXCLUSION -o /sscs-ccd.xlsx && "/wait" && "/scripts/upload-definition.sh"
