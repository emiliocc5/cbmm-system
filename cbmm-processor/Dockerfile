FROM maven:3.9.11-eclipse-temurin-25-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY ../src ./src

RUN mvn clean package

FROM openjdk:25-jdk
WORKDIR /app

COPY --from=build /app/target/processor-0.0.1.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]