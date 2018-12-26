package io.zeebe.exporter.kafka.record;

import io.zeebe.exporter.record.Record;

@FunctionalInterface
public interface RecordTransformer<T> {
  T transform(Record<?> record);
}
