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

import io.zeebe.exporters.kafka.serde.RecordId;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Partitioner} implementation which expects only {@link RecordId} objects as keys.
 *
 * <p>It will partition the records using {@link RecordId#getPartitionId()}, ensuring that all Zeebe
 * records on the same Zeebe partition will also be on the same Kafka partition, preserving the
 * ordering. It does so by taking the Zeebe partition ID (which starts at 1), and applying a modulo
 * against the number of Kafka partitions for the given topic, e.g. {@code zeebePartitionId %
 * kafkaPartitionsCount}.
 *
 * <p>One downside is that if you have more Kafka partitions than Zeebe partitions, some of your
 * partitions will be unused: partition 0, and any partition whose number is greater than the count
 * of Zeebe partitions.
 *
 * <p>For example, if you have 3 Zeebe partitions, and 2 Kafka partitions:
 *
 * <ul>
 *   <li>RecordId{partitionId=1, position=1} => Kafka partition 1
 *   <li>RecordId{partitionId=2, position=1} => Kafka partition 0
 *   <li>RecordId{partitionId=3, position=1} => Kafka partition 1
 *   <li>RecordId{partitionId=3, position=2} => Kafka partition 1
 *   <li>RecordId{partitionId=2, position=2} => Kafka partition 0
 * </ul>
 *
 * <p>With more Kafka partitions, for example, 4 Kafka partitions, and 3 Zeebe partitions:
 *
 * <ul>
 *   <li>RecordId{partitionId=1, position=1} => Kafka partition 1
 *   <li>RecordId{partitionId=2, position=1} => Kafka partition 2
 *   <li>RecordId{partitionId=3, position=1} => Kafka partition 3
 *   <li>RecordId{partitionId=3, position=2} => Kafka partition 3
 *   <li>RecordId{partitionId=2, position=2} => Kafka partition 2
 * </ul>
 */
public class RecordIdPartitioner implements Partitioner {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecordIdPartitioner.class);

  private final DefaultPartitioner defaultPartitioner = new DefaultPartitioner();

  @Override
  public int partition(
      final String topic,
      final Object key,
      final byte[] keyBytes,
      final Object value,
      final byte[] valueBytes,
      final Cluster cluster) {
    if (!(key instanceof RecordId)) {
      LOGGER.warn(
          "Expected to partition a RecordId object, but got {}; falling back to default partitioner",
          key.getClass());
      return defaultPartitioner.partition(topic, key, keyBytes, value, valueBytes, cluster);
    }

    final List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
    final int numPartitions = partitions.size();
    final RecordId recordId = (RecordId) key;
    final int partitionId = recordId.getPartitionId() % numPartitions;

    LOGGER.trace("Assigning partition {} to record ID {}", partitionId, recordId);

    return partitionId;
  }

  @Override
  public void close() {
    // do nothing
  }

  @Override
  public void configure(final Map<String, ?> configs) {
    // not configurable yet
  }
}
