# ===================================================================================
# STAGE 1: Dependencies (cacheable, no secrets)
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy AS deps

WORKDIR /app

# Copy only files needed to resolve dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Combine the creation of the dummy settings.xml and the dependency download
# into a SINGLE RUN command. This ensures the settings file is created AFTER
# the cache is mounted, making it visible to Maven.
RUN --mount=type=cache,target=/root/.m2 \
    sh -c 'echo "<settings/>" > /root/.m2/settings.xml && ./mvnw dependency:go-offline'


# ===================================================================================
# STAGE 2: Builder (uses secrets)
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy the pre-populated Maven cache from the 'deps' stage.
COPY --from=deps /root/.m2 /root/.m2
# Copy the entire project source code.
COPY . .

# Build the application JAR.
# The secret mount temporarily overwrites the dummy settings.xml with the real one.
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
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the layers from the extractor stage
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]