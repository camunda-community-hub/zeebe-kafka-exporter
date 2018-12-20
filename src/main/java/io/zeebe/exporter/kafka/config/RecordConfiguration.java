package io.zeebe.exporter.kafka.config;

public class RecordConfiguration {
  public static final String DEFAULT_TOPIC_NAME = "zeebe";
  public static final AllowedType DEFAULT_ALLOWED_TYPE = AllowedType.ALL;

  public String type = DEFAULT_ALLOWED_TYPE.name;
  public String topic = DEFAULT_TOPIC_NAME;
}
