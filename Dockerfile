FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-1.0
LABEL maintainer="https://github.com/hmcts/sscs-tribunals-case-api"

COPY build/libs/tribunals-case-api.jar /opt/app/

WORKDIR /opt/app

HEALTHCHECK NONE

EXPOSE 8080

CMD ["tribunals-case-api.jar"]
