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
package at.phactum.zeebe.exporters.kafka.producer;

import at.phactum.zeebe.exporters.kafka.KafkaExporter;
import at.phactum.zeebe.exporters.kafka.config.Config;
import at.phactum.zeebe.exporters.kafka.config.raw.RawProducerConfig;
import at.phactum.zeebe.exporters.kafka.serde.RecordId;
import at.phactum.zeebe.exporters.kafka.serde.RecordIdSerializer;

import java.util.HashMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;

/**
 * {@link DefaultKafkaProducerFactory} is the default implementation of {@link KafkaProducerFactory}
 * used by {@link KafkaExporter}. It creates a new {@link Producer} based
 * on the given {@link Config}, and adds a few default properties.
 *
 * <p>It's tuned for small, fast batching, and low memory consumption. By default, it will wait up
 * to 10ms or until it has batched 4MB (the default maxMessageSize of Zeebe) in memory before
 * sending a request. This is to lessen the load on Kafka while remaining fairly responsive.
 *
 * <p>The memory usage of the producer is soft capped to 40Mb - if you produce much faster than it
 * can export, then you may run into exceptions. In this case, you can increase the memory to
 * something you feel more comfortable with via {@link
 * RawProducerConfig#config}.
 */
final class DefaultKafkaProducerFactory implements KafkaProducerFactory {
  @Override
  public Producer<RecordId, byte[]> newProducer(
          final at.phactum.zeebe.exporters.kafka.config.ProducerConfig config, final String producerId) {
    final var options = new HashMap<String, Object>();
    final var clientId = String.format("%s-%s", config.getClientId(), producerId);

    options.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, producerId);
    options.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
    options.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

    // disable concurrent connections to ensure order is preserved
    options.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    options.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Integer.MAX_VALUE);
    options.put(
        ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) config.getRequestTimeout().toMillis());
    options.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getServers());

    // provides a soft memory bound - there's some memory overhead used by SSL, compression, etc.,
    // but this gives us a good idea of how much memory will be used by the exporter
    options.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 40 * 1024 * 1024L);

    // wait up to 10ms or until the batch is full before sending
    options.put(ProducerConfig.LINGER_MS_CONFIG, 10L);
    options.put(ProducerConfig.BATCH_SIZE_CONFIG, 4 * 1024 * 1024L);
    options.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, config.getMaxBlockingTimeout().toMillis());

    // leave always close to the last step to allow user configuration to override producer options
    options.putAll(config.getConfig());

    options.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, RecordIdSerializer.class);
    options.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    options.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, RecordIdPartitioner.class);

    return new KafkaProducer<>(options);
  }
}
