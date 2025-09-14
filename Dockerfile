FROM openjdk:17-alpine
ARG JAR_FILE=target/devtec-0.0.1-SNAPSHOT.jar
WORKDIR /app
COPY ${JAR_FILE} .
CMD ["java", "-jar", "./devtec-0.0.1-SNAPSHOT.jar"]