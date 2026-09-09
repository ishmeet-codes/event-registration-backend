# ==========================================
# Stage 1: Build
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy dependency configuration first
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests


# ==========================================
# Stage 2: Runtime
# ==========================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create non-root user
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser

# Copy only the generated JAR
COPY --from=build /app/target/*.jar app.jar

# Give ownership to application user
RUN chown appuser:appuser app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]