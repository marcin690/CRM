# --- build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Najpierw tylko pom — warstwa zależności cache'uje się, dopóki pom się nie zmieni.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# --- runtime ---
FROM eclipse-temurin:17-jre
WORKDIR /app
# Spring Boot repackuje do *.jar (plik *.jar.original pomijamy glob-em *.jar).
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
