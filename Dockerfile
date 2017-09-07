FROM openjdk:8-alpine

RUN mkdir -p /opt/sscs/tribunals-case-api

COPY target/tribunals-case-api*.jar /opt/sscs/tribunals-case-api/app.jar

ENV JAVA_OPTS=""

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -qO- "http://localhost:8080/health" | grep UP -q

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /opt/sscs/tribunals-case-api/app.jar" ]

