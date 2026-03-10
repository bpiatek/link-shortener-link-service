FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --chown=spring:spring target/extracted/dependencies/ ./
COPY --chown=spring:spring target/extracted/spring-boot-loader/ ./
COPY --chown=spring:spring target/extracted/snapshot-dependencies/ ./
COPY --chown=spring:spring target/extracted/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]