# Base Alpine Linux based image with OpenJDK JRE only
FROM openjdk:8-jre-alpine

# Copy consumer
COPY target/zeebe-kafka-exporter-*-uber.jar /consumer.jar

# specify default command
ENTRYPOINT ["/usr/bin/java", "-jar", "/consumer.jar"]

