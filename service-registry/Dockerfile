FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Optimización: Copiar primero el pom y descargar dependencias (cache)
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Corregido: --from=build y buscando el jar que NO es el 'plain'
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]