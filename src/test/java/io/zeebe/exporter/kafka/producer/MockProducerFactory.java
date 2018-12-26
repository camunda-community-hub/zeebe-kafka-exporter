package io.zeebe.exporter.kafka.producer;

import io.zeebe.exporter.kafka.config.Config;
import io.zeebe.exporter.record.Record;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;

public class MockProducerFactory implements ProducerFactory {
  public MockProducer<Record, Record> mockProducer;

  @Override
  public Producer<Record, Record> newProducer(Config config) {
    if (mockProducer == null) {
      mockProducer = new MockProducer<>();
    }

    return mockProducer;
  }
}
