ARG APP_INSIGHTS_AGENT_VERSION=2.5.0-BETA.5

FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.0
LABEL maintainer="https://github.com/hmcts/sscs-tribunals-case-api"

COPY lib/applicationinsights-agent-${APP_INSIGHTS_AGENT_VERSION}.jar lib/AI-Agent.xml /opt/app/
COPY build/libs/tribunals-case-api.jar /opt/app/

WORKDIR /opt/app

HEALTHCHECK NONE

EXPOSE 8080

CMD ["tribunals-case-api.jar"]
