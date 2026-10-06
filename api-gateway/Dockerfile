# Imagen de api-gateway: se compila con Maven y corre sobre un JRE; el contexto de build es la raiz

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /proyecto
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -pl api-gateway package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 banco
COPY --from=build /proyecto/api-gateway/target/api-gateway-1.0-SNAPSHOT.jar app.jar
USER banco
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
