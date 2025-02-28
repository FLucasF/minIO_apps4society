# Usar uma imagem oficial do OpenJDK 21
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copia o arquivo pom.xml e baixa as dependências
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:resolve

# Copia o código do projeto e compila a aplicação
COPY src ./src
RUN ./mvnw package -DskipTests

# Segunda etapa: criar a imagem final com a API Spring Boot
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/MinIO_API-0.0.1-SNAPSHOT.jar app.jar

# Define a porta que será exposta
EXPOSE 8080

# Comando para rodar a aplicação
CMD ["java", "-jar", "app.jar"]
