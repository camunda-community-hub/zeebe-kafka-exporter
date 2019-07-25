# Base Alpine Linux based image with OpenJDK JRE only
FROM openjdk:8-jre-alpine

# Install tini
RUN apk add --no-cache tini

# Copy consumer
COPY target/zeebe-kafka-exporter-*-uber.jar /usr/local/bin/consumer.jar

# Copy startup script
COPY docker/startup.sh /usr/local/bin/startup.sh

# specify default command
ENTRYPOINT ["/sbin/tini", "--", "/usr/local/bin/startup.sh"]

