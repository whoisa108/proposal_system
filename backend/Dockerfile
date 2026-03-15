# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# maven:3.9-eclipse-temurin-17-alpine already has both Maven AND JDK 17.
# We don't need to install anything extra.
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy the Maven descriptor FIRST — before source code.
# Docker builds in layers. If pom.xml hasn't changed, Docker reuses the cached
# layer where dependencies were downloaded (saves minutes on every rebuild).
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Now copy source code and build the JAR.
# -DskipTests: CI already ran tests; we don't run them again here.
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
# JRE (Java Runtime Environment) only — no compiler, no Maven.
# alpine = minimal Linux base (~5MB). Final image ≈ 180MB.
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Run as a non-root user. If the container is compromised, the attacker
# doesn't have root access to the host system.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy only the compiled JAR from the builder stage.
# Nothing else (no source, no Maven cache) ends up in the final image.
COPY --from=builder /app/target/*.jar app.jar

USER appuser

# Document which port the app listens on (doesn't actually publish it —
# that's done by Kubernetes Service).
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
