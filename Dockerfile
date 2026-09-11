FROM gradle:8.14-jdk17 AS build

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]