package io.zeebe.exporter.kafka.producer;

import io.zeebe.exporter.kafka.config.Config;
import io.zeebe.exporter.kafka.record.RecordSerializer;
import io.zeebe.exporter.record.Record;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.HashMap;
import java.util.Map;

public class KafkaProducerFactory implements ProducerFactory {
  @Override
  public Producer<Record, Record> newProducer(Config config) {
    final Map<String, Object> options = new HashMap<>();

    options.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    options.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, config.producer.maxConcurrentRequests);
    options.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Integer.MAX_VALUE);
    options.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) config.producer.requestTimeout.toMillis());
    options.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.producer.servers);
    options.put(ProducerConfig.CLIENT_ID_CONFIG, config.producer.clientId);

    // allow user configuration to override producer options
    if (config.producer.config != null) {
      options.putAll(config.producer.config);
    }

    options.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, RecordSerializer.class.getName());
    options.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RecordSerializer.class.getName());

    return new KafkaProducer<>(options);
  }
}
