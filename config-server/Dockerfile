# syntax=docker/dockerfile:1.7-labs
FROM gradle:9.3.0-jdk17 AS builder
WORKDIR /build
COPY --parents config-server ./
RUN cd config-server && gradle clean bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/config-server/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
