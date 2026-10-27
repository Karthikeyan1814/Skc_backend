# ========================================================
# Optimized Multi-Stage Dockerfile for Spring Boot
# Supports Render (512MB RAM free tier) & Railway
# Project: SKC-Backend-Files
# ========================================================

# --------------------------------------------------------
# Stage 1: Build the executable JAR with Maven
# --------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cap Maven heap so Render free tier builder does not run out of memory (OOM 137)
ENV MAVEN_OPTS="-Xms64m -Xmx384m -XX:+UseSerialGC"

# Copy pom.xml and source files
COPY pom.xml .
COPY src ./src

# Build production JAR without tests and suppress verbose download spam in Render logs
RUN mvn clean package -DskipTests -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

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

# Render and Railway automatically set the PORT environment variable
ENV PORT=8080
EXPOSE 8080

# Optimized JVM memory flags for 512MB container environments
ENV JAVA_OPTS="-Xms64m -Xmx350m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Launch Spring Boot with dynamic port binding for Render/Railway ($PORT)
# Supports Render Secret Files mounted at /etc/secrets/application.properties
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -Dspring.config.additional-location=optional:file:/etc/secrets/application.properties,optional:file:/etc/secrets/ -jar app.jar"]

