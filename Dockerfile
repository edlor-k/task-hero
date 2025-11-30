# ==========================================
# TaskHero User Service - Multi-stage Build
# ==========================================

# Stage 1: Build Application
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy all necessary files
COPY pom.xml .
COPY dependency-bom ./dependency-bom
COPY common ./common
COPY user-service ./user-service

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -pl user-service -am

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security and install wget for healthcheck
RUN addgroup -S spring && adduser -S spring -G spring && \
    apk add --no-cache wget

# Copy JAR from builder stage
COPY --from=builder /app/user-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

USER spring:spring

# Expose application port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/users/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
