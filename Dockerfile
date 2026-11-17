# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Download dependencies first, in a separate layer.
# As long as pom.xml does not change, Docker reuses this layer and rebuilds are fast.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in CI / locally; the image build only packages the app.
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: minimal runtime image ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Do not run the app as root inside the container.
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
