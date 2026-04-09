# ── Stage 1: Build ──────────────────────────────────────
FROM maven:3-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml first
COPY pom.xml .

# Download dependencies only
RUN mvn dependency:resolve -q

# Copy source
COPY src ./src

# Build JAR (no debug, no tests, quiet mode)
RUN mvn clean package -DskipTests -q

# ── Stage 2: Run ────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/course-recommender-1.0.0.jar app.jar

# Render injects PORT env var
EXPOSE 8080

# Start
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
