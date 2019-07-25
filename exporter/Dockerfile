FROM camunda/zeebe:0.20.0

# Copy sample Kafka Exporter configuration to config folder
COPY exporter.kafka.cfg.toml ${ZB_HOME}/conf/

# Append contents of Kafka Exporter config to the main configuration file
RUN cat ${ZB_HOME}/conf/exporter.kafka.cfg.toml >> ${ZB_HOME}/conf/zeebe.cfg.toml

# Copy of the JAR containing the exporter
COPY target/zeebe-kafka-exporter-*-uber.jar ${ZB_HOME}/lib/zeebe-kafka-exporter.jar
