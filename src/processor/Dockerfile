# -- Build the project
FROM maven:3.6.1-jdk-8-alpine AS builder
WORKDIR /app
COPY ./ ./
RUN mvn clean package

# -- Create project image 
FROM openjdk:8-jre-alpine3.9
COPY --from=builder /app/target/processor-1.0-SNAPSHOT.jar /processor.jar
CMD ["java", "-jar", "/processor.jar"]