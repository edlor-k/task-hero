# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom files and checkstyle for dependency caching
COPY pom.xml .
COPY checkstyle.xml .
COPY dependency-bom/pom.xml dependency-bom/pom.xml
COPY common/pom.xml common/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY task-service/pom.xml task-service/pom.xml
COPY app/pom.xml app/pom.xml

# Download dependencies (this layer will be cached)
RUN mvn dependency:go-offline -pl app -am -B -q || true

# Copy source code
COPY common/src common/src
COPY app/src app/src

# Build the application (skip tests and checkstyle)
RUN mvn package -pl app -am -DskipTests -Dcheckstyle.skip=true -B -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add curl for healthcheck
RUN apk add --no-cache curl

# Copy the jar
COPY --from=build /app/app/target/*.jar app.jar

# Create non-root user
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+UseStringDeduplication", \
  "-XX:MinHeapFreeRatio=20", \
  "-XX:MaxHeapFreeRatio=40", \
  "-jar", "app.jar"]
