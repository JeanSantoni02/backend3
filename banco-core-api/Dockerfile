# Imagen de banco-core-api
# Se compila con Maven y se ejecuta sobre un JRE, para que la imagen final no
# cargue con el compilador. El contexto de build es la raiz del proyecto.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /proyecto
COPY . .
# La cache del repositorio local se comparte entre imagenes y evita volver a
# descargar las dependencias en cada servicio
RUN --mount=type=cache,target=/root/.m2     mvn -q -B -pl banco-core-api package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 banco
COPY --from=build /proyecto/banco-core-api/target/banco-core-api-1.0-SNAPSHOT.jar app.jar
USER banco
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
