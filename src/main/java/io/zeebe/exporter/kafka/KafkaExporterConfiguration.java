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

import io.zeebe.exporter.record.Record;
import io.zeebe.util.ByteValue;
import io.zeebe.util.DurationUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.record.CompressionType;

public class KafkaExporterConfiguration {
  private static final String CLIENT_ID_FORMAT = "zb-kafka-exporter-%s";

  private final RecordSerializer keySerializer = new RecordSerializer();
  private final RecordSerializer valueSerializer = new RecordSerializer();

  public String topic;
  public List<String> servers = Collections.singletonList("localhost:9092");
  public int maxInFlightRecords = 1_000;
  public ProducerConfiguration producer = new ProducerConfiguration();

  public Producer<Record, Record> newProducer(String exporterId) {
    final Map<String, Object> config = producer.newConfig();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
    config.put(ProducerConfig.CLIENT_ID_CONFIG, String.format(CLIENT_ID_FORMAT, exporterId));

    keySerializer.configure(config, true);
    valueSerializer.configure(config, false);

    return new KafkaProducer<>(config, keySerializer, valueSerializer);
  }

  @Override
  public String toString() {
    return "ExporterConfiguration{"
        + "servers="
        + servers
        + ", maxInFlightRecords="
        + maxInFlightRecords
        + ", producer="
        + producer
        + ", topic='"
        + topic
        + '\''
        + '}';
  }

  public class ProducerConfiguration {
    public String batchSize = "128K";
    public String batchLinger = "100ms";
    public String compressionType = CompressionType.SNAPPY.name;
    public String requestTimeout = "10s";
    public int maxConcurrentRequests = 3;
    public Map<String, String> extra = new HashMap<>();

    /**
     * Configures the producer to be idempotent and deliver messages in order. setting idempotence
     * to true defaults retries to {@link Integer.MAX_VALUE} and acks to all. This also restricts
     * the number of maximum in flight requests per connection to at most 5.
     *
     * @return producer configuration
     */
    private Map<String, Object> newConfig() {
      final Map<String, Object> config = new HashMap<>(extra);

      config.put(ProducerConfig.BATCH_SIZE_CONFIG, (int) new ByteValue(batchSize).toBytes());
      config.put(ProducerConfig.LINGER_MS_CONFIG, (int) DurationUtil.parse(batchLinger).toMillis());
      config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
      config.put(
          ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
          (int) DurationUtil.parse(requestTimeout).toMillis());
      config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
      config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxConcurrentRequests);

      return config;
    }

    @Override
    public String toString() {
      return "ProducerConfiguration{"
          + "batchSize='"
          + batchSize
          + '\''
          + ", batchLinger='"
          + batchLinger
          + '\''
          + ", compressionType='"
          + compressionType
          + '\''
          + ", requestTimeout='"
          + requestTimeout
          + '\''
          + ", extra="
          + extra
          + '}';
    }
  }
}
