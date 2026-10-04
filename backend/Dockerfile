# Multi-stage Dockerfile for Spring Boot Backend
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
COPY src ./src
# Build application jar skipping tests for container packaging
RUN ./mvnw clean package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Install multi-language execution runtimes (python3, nodejs, g++) and build tools
RUN apk add --no-cache python3 nodejs g++
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xmx256m", "-Xms128m", "-XX:MaxMetaspaceSize=128m", "-XX:ReservedCodeCacheSize=64m", "-Xss512k", "-jar", "app.jar"]
