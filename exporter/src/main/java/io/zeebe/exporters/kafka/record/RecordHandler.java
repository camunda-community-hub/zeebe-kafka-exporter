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
package io.zeebe.exporters.kafka.record;

import io.camunda.zeebe.protocol.record.Record;
import io.zeebe.exporters.kafka.config.RecordConfig;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.exporters.kafka.serde.RecordId;
import java.util.Objects;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;

/**
 * {@link RecordHandler} is responsible for testing if certain records are allowed, and if so,
 * transforming them.
 *
 * <p>Should be refactored into two for single responsibility.
 */
public final class RecordHandler {
  private final RecordsConfig configuration;
  private final Serializer<Record<?>> serializer;

  public RecordHandler(final RecordsConfig configuration) {
    this(configuration, new RecordSerializer());
  }

  public RecordHandler(final RecordsConfig configuration, final Serializer<Record<?>> serializer) {
    this.configuration = Objects.requireNonNull(configuration);
    this.serializer = Objects.requireNonNull(serializer);
  }

  /**
   * Transforms the given {@link Record} into a Kafka {@link ProducerRecord}.
   *
   * @param record the record to transform
   * @return the transformed record
   */
  public ProducerRecord<RecordId, byte[]> transform(final Record record) {
    final RecordConfig config = getRecordConfig(record);
    final byte[] serializedRecord = serializer.serialize(config.getTopic(), record);
    return new ProducerRecord<>(
        config.getTopic(),
        new RecordId(record.getPartitionId(), record.getPosition()),
        serializedRecord);
  }

  /**
   * Tests whether or not the given record is allowed, as specified by the configuration.
   *
   * @param record the record to test
   * @return true if allowed, false otherwise
   */
  public boolean isAllowed(final Record<?> record) {
    final RecordConfig config = getRecordConfig(record);
    return config.getAllowedTypes().contains(record.getRecordType());
  }

  private RecordConfig getRecordConfig(final Record<?> record) {
    return configuration.forType(Objects.requireNonNull(record).getValueType());
  }
}
