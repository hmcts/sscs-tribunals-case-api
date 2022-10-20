ARG APP_INSIGHTS_AGENT_VERSION=2.6.4
FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/tribunals-case-api.jar /opt/app/

EXPOSE 8080

CMD ["tribunals-case-api.jar"]
