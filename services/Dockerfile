# Build any Spring Boot module from this repo.
# Usage (compose): build-arg SERVICE_DIR=auth-service
#
# Order at runtime is handled by docker-compose depends_on + healthchecks,
# not by this image (one container = one service).

ARG SERVICE_DIR=auth-service

FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG SERVICE_DIR
WORKDIR /src

# Shared library (user-service, order-service, api-gateway)
COPY common-security/pom.xml common-security/pom.xml
COPY common-security/src common-security/src
RUN mvn -q -f common-security/pom.xml install -DskipTests

# Selected microservice
COPY ${SERVICE_DIR}/pom.xml service/pom.xml
COPY ${SERVICE_DIR}/src service/src
COPY ${SERVICE_DIR}/mvnw service/mvnw
COPY ${SERVICE_DIR}/.mvn service/.mvn
RUN chmod +x service/mvnw \
	&& mvn -q -f service/pom.xml package -DskipTests \
	&& cp service/target/*.jar /src/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/*
COPY --from=build /src/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
