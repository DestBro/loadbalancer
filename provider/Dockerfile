FROM openjdk:8-jdk-alpine
ARG PORT
ENV ENV_PORT=$PORT
COPY provider/target/*.jar app.jar
ENTRYPOINT ["java","-Dserver.port=${ENV_PORT}", "-jar","/app.jar" ]