# ===================================================================================
# STAGE 1: Dependencies (cacheable, no secrets)
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy AS deps

WORKDIR /app

# Copy only files needed to resolve dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# THIS IS THE FIRST CHANGE:
# We give the cache mount a specific ID: 'maven-cache'.
RUN --mount=type=cache,id=maven-cache,target=/root/.m2 \
    sh -c 'echo "<settings/>" > /root/.m2/settings.xml && ./mvnw dependency:go-offline'


# ===================================================================================
# STAGE 2: Builder (uses secrets)
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# THIS IS THE SECOND CHANGE:
# We REMOVE the COPY command for the .m2 directory. It is no longer needed
# because the shared cache mount will provide the dependencies.
# COPY --from=deps /root/.m2 /root/.m2  <-- DELETE THIS LINE
COPY . .

# THIS IS THE THIRD CHANGE:
# We use the SAME cache mount ID: 'maven-cache'.
# Docker will now mount the exact same volume that was populated in the 'deps' stage.
RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    --mount=type=cache,id=maven-cache,target=/root/.m2 \
    ./mvnw package -DskipTests --global-settings /root/.m2/settings.xml


# ===================================================================================
# STAGE 3 & 4 (NO CHANGES NEEDED)
# ===================================================================================
FROM builder AS extractor
# ... (rest of your Dockerfile is the same)