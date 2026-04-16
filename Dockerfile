# -------- Build stage --------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .

COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw || true
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests clean package

# -------- Runtime stage --------
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/msmeApp

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# JVM flags for container awareness
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
