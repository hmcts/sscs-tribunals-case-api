FROM openjdk:8-jre

COPY build/libs/tribunals-case-api.jar /opt/app/

WORKDIR /opt/app

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" curl --silent --fail http://localhost:8082/health

EXPOSE 8082

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/app/tribunals-case-api.jar"]
