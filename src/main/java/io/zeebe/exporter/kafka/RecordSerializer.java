package io.zeebe.exporter.kafka;

import io.zeebe.exporter.record.Record;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;

public class RecordSerializer implements Serializer<Record> {
  private final StringSerializer serializer = new StringSerializer();

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {}

  @Override
  public byte[] serialize(String topic, Record data) {
    return serializer.serialize(topic, data.toJson());
  }

  @Override
  public void close() {}
}
