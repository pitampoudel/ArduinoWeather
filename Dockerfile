# Multi-stage build for Spring Boot Kotlin application
# Stage 1: Build the application
FROM gradle:8.5-jdk17 AS builder

# Set working directory
WORKDIR /app

# Copy gradle files first for better caching
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle

# Copy source code
COPY src src

# Build the application
RUN gradle build --no-daemon -x test

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre

# Install necessary packages and create non-root user
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port 8080 (GCP Cloud Run default)
EXPOSE 8080

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check (GCP Cloud Run will handle health checks automatically)
# HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#     CMD curl -f http://localhost:8080/ || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
