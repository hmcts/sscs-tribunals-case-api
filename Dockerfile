ARG APP_INSIGHTS_AGENT_VERSION=3.4.8
FROM hmctspublic.azurecr.io/base/java:11-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/tribunals-case-api.jar /opt/app/

EXPOSE 8080

CMD ["tribunals-case-api.jar"]
