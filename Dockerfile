FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --chown=spring:spring docker-layers/dependencies/ ./
COPY --chown=spring:spring docker-layers/spring-boot-loader/ ./
COPY --chown=spring:spring docker-layers/snapshot-dependencies/ ./
COPY --chown=spring:spring docker-layers/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]