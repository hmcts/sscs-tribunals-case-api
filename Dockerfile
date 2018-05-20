FROM openjdk:8-jre

COPY build/libs/tribunals-case-api.jar /opt/app/

WORKDIR /opt/app

HEALTHCHECK NONE

EXPOSE 8082

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/app/tribunals-case-api.jar"]
