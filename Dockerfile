FROM openjdk:8-jre

COPY build/install/tribunals-case-api /opt/app/

WORKDIR /opt/app

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" curl --silent --fail http://localhost:8083/health

EXPOSE 8083

ENTRYPOINT ["/opt/app/bin/tribunals-case-api"]
