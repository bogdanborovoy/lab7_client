FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/client-1.0-SNAPSHOT-jar-with-dependencies.jar client.jar
ENTRYPOINT ["java", "-jar", "client.jar"]