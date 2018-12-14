FROM camunda/zeebe:0.14.0

ARG version

# Copy sample Kafka Exporter configuration to config folder
COPY exporter.kafka.cfg.toml ${ZB_HOME}/conf/

# Append contents of Kafka Exporter config to the main configuration file
RUN cat ${ZB_HOME}/conf/exporter.kafka.cfg.toml >> ${ZB_HOME}/conf/zeebe.cfg.toml

# Copy of the JAR containing the exporter
COPY target/zb-kafka-exporter-$version.jar ${ZB_HOME}/lib/zb-kafka-exporter.jar
