# =============================================================================
# AnchorPQ Server Dockerfile
# Multi-stage build for optimized production image
# =============================================================================

# =============================================================================
# Stage 1: Build
# =============================================================================
FROM --platform=$BUILDPLATFORM maven:3-eclipse-temurin-25 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (fast-jar is default for Quarkus)
RUN mvn package -DskipTests


# =============================================================================
# Stage 2: Production Runtime
# =============================================================================
FROM eclipse-temurin:17-jre-jammy

# Labels
LABEL maintainer="AnchorPQ Team"
LABEL description="AnchorPQ Post-Quantum Secure Integrity Verification Server"
LABEL version="1.0.0"

# Install wget for health check + create non-root user
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r anchorpq \
    && useradd -r -g anchorpq anchorpq

# Set working directory
WORKDIR /app

# Copy Quarkus fast-jar output
COPY --from=build /app/target/quarkus-app/lib/ /app/lib/
COPY --from=build /app/target/quarkus-app/*.jar /app/
COPY --from=build /app/target/quarkus-app/app/ /app/app/
COPY --from=build /app/target/quarkus-app/quarkus/ /app/quarkus/

# Create directory for optional key persistence
RUN mkdir -p /app/keys && chown -R anchorpq:anchorpq /app

# Switch to non-root user
USER anchorpq

# Expose port
EXPOSE 8080

# Health check (Quarkus SmallRye Health)
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/q/health/ready || exit 1

# JVM tuning (good baseline for containers)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
ENV QUARKUS_HTTP_HOST="0.0.0.0"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/quarkus-run.jar"]

