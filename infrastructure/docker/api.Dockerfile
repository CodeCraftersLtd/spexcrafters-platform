# SpexCrafters API — multi-stage build
# Build context: repository root
#   docker build -f infrastructure/docker/api.Dockerfile -t spexcrafters/api .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY apps/api/pom.xml apps/api/pom.xml
COPY apps/api/modules apps/api/modules
COPY apps/api/application apps/api/application
COPY apps/api/architecture-tests apps/api/architecture-tests
RUN mvn -f apps/api/pom.xml -B -DskipTests package

# Alpine variant: the Ubuntu-based -jre image ships /usr/bin/pebble (Go binary)
# which carries HIGH CVEs unrelated to this application (first Trivy scan).
FROM eclipse-temurin:25-jre-alpine AS runtime
# Base images lag Alpine security updates between rebuilds; pull the patched
# OS packages at build time (first Trivy scan: libexpat et al. HIGH, fixed).
RUN apk upgrade --no-cache \
 && adduser -S -u 10001 spexcrafters
USER 10001
WORKDIR /app
COPY --from=build /workspace/apps/api/application/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
HEALTHCHECK --interval=15s --timeout=3s --retries=10 \
  CMD ["sh", "-c", "wget -qO- http://localhost:8080/actuator/health/liveness || exit 1"]
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
