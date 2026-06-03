# ---- Etapa 1: build con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom para cachear las dependencias entre builds.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Ahora el codigo y empaquetado (sin tests para acelerar la imagen).
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Etapa 2: runtime liviano (solo JRE) ----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# curl para el healthcheck; usuario sin privilegios.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
