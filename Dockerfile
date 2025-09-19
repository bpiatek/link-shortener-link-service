# ===================================================================================
# STAGE 1: The "Builder" Stage
# ===================================================================================
FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /app

# Copy only the files needed to download dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:resolve --global-settings /root/.m2/settings.xml

# Copy the rest of the source code
COPY src ./src

RUN --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests --global-settings /root/.m2/settings.xml

# ===================================================================================
# STAGE 2: The "Extractor"
# ===================================================================================
FROM builder as extractor

COPY --from=builder /app/target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

# ===================================================================================
# STAGE 3: The Final Image
# ===================================================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]