# Usar uma imagem oficial do OpenJDK 21
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:resolve

COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/MinIO_API-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
