FROM maven:3.8.6-openjdk-11-slim as MAVEN_TOOL_CHAIN_CACHE
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline

COPY ./pom.xml /tmp/
COPY ./src /tmp/src/
WORKDIR /tmp/
RUN mvn clean package -Dmaven.test.skip

FROM openjdk:11-slim

EXPOSE 9797

ENV JAVA_OPTS=""
COPY --from=MAVEN_TOOL_CHAIN_CACHE /tmp/target/keycloak-springboot-microservice-0.0.1-SNAPSHOT.jar /app.jar

RUN sh -c 'touch /app.jar'

CMD [ "java", "-jar" ,"app.jar" ]
