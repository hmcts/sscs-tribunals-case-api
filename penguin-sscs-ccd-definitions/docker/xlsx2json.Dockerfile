# ---- Base image - order important ----
FROM hmcts/ccd-definition-processor:latest as base

# ----        Runtime image         ----
FROM hmcts/ccd-definition-importer:latest as runtime
COPY --from=base . .
RUN apk add --no-cache curl jq zip unzip git
CMD cd /opt/ccd-definition-processor && mkdir -p /data/sheets && yarn xlsx2json -D /data/sheets -i /data/sscs-ccd.xlsx