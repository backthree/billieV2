FROM eclipse-temurin:21-jre

ARG JAR_FILE=build/libs/*.jar

WORKDIR /app

COPY ${JAR_FILE} app.jar

EXPOSE 8081

ENV ACTIVE_PROFILE=prod

ENTRYPOINT ["java","-jar","app.jar"]
