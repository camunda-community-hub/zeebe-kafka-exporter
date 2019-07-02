/*
 * Copyright © 2019 camunda services GmbH (info@camunda.com)
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
package io.zeebe.exporters.kafka.producer;

import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.serde.RecordIdSerializer;
import io.zeebe.exporters.kafka.serde.generic.GenericRecordSerializer;
import io.zeebe.protocol.record.Record;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;

public class DefaultKafkaProducerFactory implements KafkaProducerFactory {
  @Override
  public Producer<Record, Record> newProducer(Config config) {
    final Map<String, Object> options = new HashMap<>();

    options.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    options.put(
        ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
        config.producer.maxConcurrentRequests);
    options.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Integer.MAX_VALUE);
    options.put(
        ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) config.producer.requestTimeout.toMillis());
    options.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.producer.servers);
    options.put(ProducerConfig.CLIENT_ID_CONFIG, config.producer.clientId);

    // allow user configuration to override producer options
    if (config.producer.config != null) {
      options.putAll(config.producer.config);
    }

    options.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, RecordIdSerializer.class);
    options.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GenericRecordSerializer.class);

    return new KafkaProducer<>(options);
  }
}
