# syntax=docker/dockerfile:1

FROM gradle:8.9.0-jdk17 AS build
WORKDIR /home/gradle/project
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/install/java-log-producer/ ./
ENV JAVA_LOG_PATH=/logs/java-app.log
ENV LOG_INTERVAL_SECONDS=0.1
ENV APP_NAME=java-log-producer
ENTRYPOINT ["./bin/java-log-producer"]
