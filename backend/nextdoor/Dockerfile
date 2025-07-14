FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY . .

RUN chmod +x ./gradlew && ./gradlew bootJar


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8081

ENV ACTIVE_PROFILE=prod

ENTRYPOINT ["java","-jar","app.jar"]