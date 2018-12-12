package io.zeebe.exporter.kafka;

import io.zeebe.exporter.record.Record;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public class RecordKeySerializer implements Serializer<Record> {
  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {}

  @Override
  public byte[] serialize(String topic, Record record) {
    final ByteBuffer buffer = newKeyBuffer();

    return buffer
        .order(ByteOrder.BIG_ENDIAN)
        .putInt(record.getMetadata().getPartitionId())
        .putLong(record.getKey())
        .array();
  }

  @Override
  public void close() {}

  private ByteBuffer newKeyBuffer() {
    return ByteBuffer.allocate(Long.BYTES + Integer.BYTES);
  }
}
