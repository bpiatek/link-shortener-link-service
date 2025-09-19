# ===================================================================================
# STAGE 1: Dependencies (cacheable, no secrets)
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy AS deps

WORKDIR /app

# Copy only files needed to resolve dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Ensure no settings.xml in cache
RUN rm -f /root/.m2/settings.xml || true

# Warm up Maven cache using mvnw (no secrets)
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline


# ===================================================================================
# STAGE 2: Builder (uses secrets)
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Maven cache from deps stage
COPY --from=deps /root/.m2 /root/.m2
COPY . .

# Build application with secret settings.xml
RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests --global-settings /root/.m2/settings.xml


# ===================================================================================
# STAGE 3: Extractor
# ===================================================================================
FROM builder AS extractor

WORKDIR /app

COPY --from=builder /app/target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract


# ===================================================================================
# STAGE 4: Final image
# ===================================================================================
FROM eclipse-temurin:21-jre-jammy AS final

WORKDIR /app

COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]