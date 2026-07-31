# syntax=docker/dockerfile:1
#
# Multi-stage build for the URL shortener API.
#
# Changes from the previous version, and why each matters:
#   * Dependencies are resolved in their own layer, before the source is copied.
#     Previously any one-line code change re-downloaded the entire dependency tree,
#     turning a rebuild into several minutes.
#   * The runtime image is a JRE, not a JDK. A JDK ships a compiler and tooling that
#     nothing needs at runtime; dropping it removes roughly 150MB and a meaningful
#     amount of attack surface.
#   * The process runs as a non-root user. A container escape from a root process
#     lands as root on the host.
#   * The Maven cache is a BuildKit cache mount, so it survives between builds
#     without ending up in the image.

# ------------------------------------------------------------------
# Stage 1 - build
# ------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy only the descriptor first. This layer is invalidated solely by a pom.xml
# change, so editing Java code reuses the cached dependency download.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline

COPY src ./src

# Tests run in CI, not in the image build: a failing database connection inside a
# build container would block deployment for reasons unrelated to the code.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -DskipTests

# ------------------------------------------------------------------
# Stage 2 - runtime
# ------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# curl is needed by the compose healthcheck; the rest of the image stays minimal.
RUN apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S -G spring spring

WORKDIR /app

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring

EXPOSE 8080

# MaxRAMPercentage rather than a fixed -Xmx: the JVM then sizes its heap from the
# container's actual memory limit, so the same image behaves correctly whether it
# is given 512MB or 4GB, without being retuned.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"

# Exec form via sh -c so JAVA_OPTS is word-split, while the JVM still receives
# signals correctly for a graceful shutdown.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
