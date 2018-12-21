/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.exporter.kafka;

import io.zeebe.exporter.kafka.config.AsyncConfig;
import io.zeebe.exporter.kafka.config.ClientConfig;
import io.zeebe.exporter.kafka.config.RecordsConfig;
import io.zeebe.exporter.record.Record;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;

public class KafkaExporterConfig {
  public AsyncConfig async = new AsyncConfig();
  public ClientConfig client = new ClientConfig();
  public RecordsConfig records = new RecordsConfig();

  public KafkaExporterConfig() {}

  public KafkaExporterConfig(AsyncConfig async, ClientConfig client, RecordsConfig records) {
    this.async = async;
    this.client = client;
    this.records = records;
  }

  public Producer<Record, Record> newProducer() {
    final Map<String, Object> producerConfig = newProducerConfig();
    producerConfig.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, RecordSerializer.class.getName());
    producerConfig.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RecordSerializer.class.getName());

    return new KafkaProducer<>(producerConfig);
  }

  public Map<String, Object> newProducerConfig() {
    final Map<String, Object> properties = new HashMap<>();

    // enable client side idempotence
    // configured such that we keep retrying a record "forever", even though single requests might
    // timeout and be retried
    properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    properties.put(
        ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, client.maxConcurrentRequests);
    properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Integer.MAX_VALUE);

    // allows overriding non configurable options
    if (client.config != null) {
      properties.putAll(client.config);
    }

    properties.put(
        ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) client.requestTimeout.toMillis());
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, client.servers);
    properties.put(ProducerConfig.CLIENT_ID_CONFIG, client.clientId);

    return properties;
  }
}
