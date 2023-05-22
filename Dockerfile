ARG APP_INSIGHTS_AGENT_VERSION=3.4.12
FROM hmctspublic.azurecr.io/base/java:17-distroless-debug

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/tribunals-case-api.jar /opt/app/

EXPOSE 8080

CMD ["tribunals-case-api.jar"]
