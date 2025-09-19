# ===================================================================================
# STAGE 1: Dependencies (cacheable, no secrets)
# This stage downloads all PUBLIC dependencies from Maven Central into a cacheable layer.
# It does not require any secrets.
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy AS deps

WORKDIR /app

# Copy only the files needed to resolve dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# FIX: Create a minimal, valid, but empty settings.xml file.
# This is required to prevent the 'mvnw' command from failing because it expects
# a settings file to exist, even if it's not needed for this step.
RUN mkdir -p /root/.m2 && echo "<settings/>" > /root/.m2/settings.xml

# Warm up the Maven cache by downloading all public dependencies.
# This layer will be cached and reused as long as pom.xml doesn't change.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline


# ===================================================================================
# STAGE 2: Builder (uses secrets)
# This stage builds the actual application. It uses the cache from the 'deps' stage
# and securely mounts the real settings.xml to download private dependencies.
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy the pre-populated Maven cache from the 'deps' stage.
COPY --from=deps /root/.m2 /root/.m2
# Copy the entire project source code.
COPY . .

# Build the application JAR.
# The secret mount temporarily overwrites the dummy settings.xml with the real one.
# The cache mount ensures we don't re-download public dependencies.
RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests --global-settings /root/.m2/settings.xml


# ===================================================================================
# STAGE 3: Extractor (no changes needed)
# This stage uses Spring Boot's layertools to explode the JAR for better caching.
# ===================================================================================
FROM builder AS extractor

WORKDIR /app

COPY --from=builder /app/target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract


# ===================================================================================
# STAGE 4: Final image (no changes needed)
# This stage builds the final, minimal image using only the necessary JRE
# and the exploded application layers.
# ===================================================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the layers from the extractor stage in order of least to most frequently changing
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

# The entrypoint uses the JarLauncher to run the exploded application.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]