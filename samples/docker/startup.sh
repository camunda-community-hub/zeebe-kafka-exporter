#!/bin/sh

export KAFKA_HOST=${KAFKA_HOST:-kafka:9092}
exec /usr/bin/java -Dbootstrap.servers="$KAFKA_HOST" -jar /usr/local/bin/consumer.jar
