# ===================================================================================
# STAGE 1: Builder
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /app

# 1. Copy only the files needed to resolve dependencies.
# This layer will be cached as long as pom.xml doesn't change.
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# 2. Resolve dependencies.
# This layer will use the cache as long as the previous layer was cached.
# It uses the secret settings.xml to download private dependencies.
RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:resolve --global-settings /root/.m2/settings.xml

# 3. Copy the source code.
# This layer will only be rebuilt if your Java code changes.
COPY src ./src

# 4. Build the application JAR.
# This will be very fast because all dependencies are already in the cache.
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