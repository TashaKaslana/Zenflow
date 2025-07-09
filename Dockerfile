FROM gradle:8.12.1-jdk23-alpine AS builder

WORKDIR /app

COPY gradlew gradlew
COPY settings.gradle settings.gradle
COPY gradle gradle
COPY build.gradle build.gradle

RUN chmod +x ./gradlew

RUN ./gradlew dependencies --no-daemon

COPY src src

#skip test cause i won't resolve test env placeholder
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:23-jdk-alpine AS runtime

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
