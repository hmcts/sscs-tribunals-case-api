FROM gradle:jdk8 as builder

COPY . /home/gradle/src
USER root
RUN chown -R gradle:gradle /home/gradle/src
USER gradle

WORKDIR /home/gradle/src
RUN gradle assemble

FROM openjdk:8-jre-alpine

COPY --from=builder /home/gradle/src/build/libs/tribunals-case-api.jar /opt/app/

WORKDIR /opt/app

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" curl --silent --fail http://localhost:8082/health

EXPOSE 8082

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/app/tribunals-case-api.jar"]