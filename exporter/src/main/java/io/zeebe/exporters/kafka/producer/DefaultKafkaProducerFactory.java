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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.serde.ProtobufRecordSerializer;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.serde.RecordIdSerializer;
import io.zeebe.exporters.kafka.serde.RecordSerializer;
import io.zeebe.protocol.record.Record;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;

/**
 * {@link DefaultKafkaProducerFactory} is the default implementation of {@link KafkaProducerFactory}
 * used by {@link io.zeebe.exporters.kafka.KafkaExporter}. It creates a new {@link Producer} based
 * on the given {@link Config}, and adds a few default properties.
 *
 * <p>It's tuned for small, fast batching, and low memory consumption. By default, it will wait up
 * to 10ms or until it has batched 32Kb in memory before sending a request. This is to lessen the
 * load on Kafka while remaining fairly responsive.
 *
 * <p>The memory usage of the producer is soft capped to 8Mb - if you produce much faster than it
 * can export, then you may run into exceptions. In this case, you can increase the memory to
 * something you feel more comfortable with via {@link
 * io.zeebe.exporters.kafka.config.raw.RawProducerConfig#config}.
 */
public final class DefaultKafkaProducerFactory implements KafkaProducerFactory {
  @SuppressWarnings("rawtypes")
  @Override
  public @NonNull Producer<RecordId, Record> newProducer(final @NonNull Config config) {
    final Map<String, Object> options = new HashMap<>();

    options.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    // since we're using "infinite" retries/delivery with an idempotent producer, setting the max
    // in flight requests to 1 ensures batches are delivered in order.
    options.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    options.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Integer.MAX_VALUE);
    options.put(
        ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
        (int) config.getProducer().getRequestTimeout().toMillis());
    options.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getProducer().getServers());
    options.put(ProducerConfig.CLIENT_ID_CONFIG, config.getProducer().getClientId());

    // provides a soft memory bound - there's some memory overhead used by SSL, compression, etc.,
    // but this gives us a good idea of how much memory will be used by the exporter
    options.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 8 * 1024 * 1024L);

    // wait up to 10ms or until the batch is full before sending
    options.put(ProducerConfig.LINGER_MS_CONFIG, 10L);
    options.put(ProducerConfig.BATCH_SIZE_CONFIG, 512 * 1024);

    // determines how long to block if the buffer memory is full before throwing an exception
    options.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5_000L);

    // allow user configuration to override producer options
    options.putAll(config.getProducer().getConfig());

    options.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, RecordIdSerializer.class);
    switch (config.getProducer().getFormat()) {
      case JSON:
        options.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RecordSerializer.class);
        break;
      case PROTOBUF:
        options.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ProtobufRecordSerializer.class);
        break;
      default:
        throw new IllegalArgumentException(String.format("Expected format to be one of JSON or PROTOBUF, but got %s", config.getProducer().getFormat()));
    }

    return new KafkaProducer<>(options);
  }
}
