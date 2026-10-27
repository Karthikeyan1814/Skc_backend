# ========================================================
# Multi-Stage Dockerfile for Spring Boot (Railway Ready)
# Project: SKC-Backend-Files
# ========================================================

# --------------------------------------------------------
# Stage 1: Build the executable JAR with Maven
# --------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first to speed up subsequent Docker builds
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy project source code
COPY src ./src

# Build production JAR without running tests
RUN mvn clean package -DskipTests -B

# --------------------------------------------------------
# Stage 2: Minimal, secure JRE runtime
# --------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create dedicated non-root user for security best practices
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy compiled JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Railway automatically sets the PORT environment variable
ENV PORT=8080
EXPOSE 8080

# Optimized JVM memory flags for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Launch Spring Boot with dynamic port binding for Railway ($PORT)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
