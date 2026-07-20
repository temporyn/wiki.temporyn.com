# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar \
    && find build/libs -maxdepth 1 -name "*.jar" ! -name "*-plain.jar" -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -g 1000 kkmaddress && adduser -D -u 1000 -G kkmaddress kkmaddress
COPY --from=build /workspace/app.jar ./app.jar
RUN mkdir -p logs/auth && chown -R kkmaddress:kkmaddress /app
USER kkmaddress

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
