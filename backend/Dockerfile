# ── Stage 1: Build ──────────────────────────────────────
FROM maven:3-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (fail fast if error)
RUN mvn clean dependency:resolve dependency:resolve-plugins -DskipTests

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -X 2>&1 | head -1000

# Verify JAR was created
RUN ls -la /app/target/*.jar || (echo "Build failed - no JAR found!" && exit 1)

# ── Stage 2: Run ────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/course-recommender-*.jar app.jar

# Render injects PORT env var; Spring reads it via server.port=${PORT:8080}
EXPOSE 8080

# Start application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
