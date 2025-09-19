# ===================================================================================
# STAGE 1: Builder
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Set Maven memory limits to avoid resource issues
ENV MAVEN_OPTS="-Xmx512m"

# Install Maven explicitly
RUN apt-get update && apt-get install -y maven

# 1. Copy only the files needed to resolve dependencies.
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# 2. Verify network connectivity to repositories.
RUN curl -v https://repo.bpiatek.pl/releases || echo "Failed to reach Reposilite (public)"
RUN curl -v https://packages.confluent.io/maven/ || echo "Failed to reach Confluent"
RUN curl -v https://repo1.maven.org/maven2/ || echo "Failed to reach Maven Central"

# 3. Verify settings.xml is mounted correctly.
RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    cat /root/.m2/settings.xml && echo "settings.xml mounted successfully" || echo "Failed to mount settings.xml"

# 4. Debug environment, permissions, and Maven version.
RUN env && ls -l /app && ls -l /root/.m2 && mvn --version

# 5. Resolve dependencies with verbose output, log to file, with timeout.
RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    timeout 120 mvn dependency:resolve -X -e -U --global-settings /root/.m2/settings.xml > maven-docker-log.txt 2>&1; \
    cat maven-docker-log.txt || echo "Maven command failed"

# 6. Copy the source code.
COPY src ./src

# 7. Build the application JAR.
RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    mvn package -DskipTests --global-settings /root/.m2/settings.xml

# ===================================================================================
# STAGE 2: Final Image
# ===================================================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy only the final JAR from the builder stage.
COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]