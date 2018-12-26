package io.zeebe.exporter.kafka.producer;

import io.zeebe.exporter.kafka.config.Config;
import io.zeebe.exporter.record.Record;
import org.apache.kafka.clients.producer.Producer;

@FunctionalInterface
public interface ProducerFactory {
  Producer<Record, Record> newProducer(Config config);
}
