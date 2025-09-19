# ===================================================================================
# STAGE 1: Builder
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# 1. Copy only the files needed to resolve dependencies.
# This layer will be cached as long as pom.xml doesn't change.
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# 2. Verify network connectivity to repositories.
RUN curl -v https://repo.bpiatek.pl/releases || echo "Failed to reach Reposilite (public)"
RUN curl -v https://packages.confluent.io/maven/ || echo "Failed to reach Confluent"
RUN curl -v https://repo1.maven.org/maven2/ || echo "Failed to reach Maven Central"

# 3. Verify settings.xml is mounted correctly.
RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    cat /root/.m2/settings.xml && echo "settings.xml mounted successfully" || echo "Failed to mount settings.xml"

# 4. Resolve dependencies with verbose output.
# Using dependency:go-offline to fetch all dependencies and plugins.
RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -X --global-settings /root/.m2/settings.xml

# 5. Copy the source code.
# This layer will only be rebuilt if your Java code changes.
COPY src ./src

# 6. Build the application JAR.
# This will be fast because dependencies are already cached.
RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests --global-settings /root/.m2/settings.xml

# ===================================================================================
# STAGE 2: Final Image
# ===================================================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy only the final JAR from the builder stage.
COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]