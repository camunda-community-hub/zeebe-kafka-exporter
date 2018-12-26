package io.zeebe.exporter.kafka.record;

import io.zeebe.exporter.kafka.config.RecordConfig;
import io.zeebe.exporter.kafka.config.RecordsConfig;
import io.zeebe.exporter.record.Record;
import org.apache.kafka.clients.producer.ProducerRecord;

public class RecordHandler
  implements RecordTester, RecordTransformer<ProducerRecord<Record, Record>> {
  private final RecordsConfig configuration;

  public RecordHandler(RecordsConfig configuration) {
    this.configuration = configuration;
  }

  @Override
  public ProducerRecord<Record, Record> transform(Record record) {
    final RecordConfig config = getRecordConfig(record);
    return new ProducerRecord<>(config.topic, record, record);
  }

  @Override
  public boolean test(Record record) {
    final RecordConfig config = getRecordConfig(record);
    return config.allowedTypes.contains(record.getMetadata().getRecordType());
  }

  private <T extends Record> RecordConfig getRecordConfig(T record) {
    return configuration.forType(record.getMetadata().getValueType());
  }
}
