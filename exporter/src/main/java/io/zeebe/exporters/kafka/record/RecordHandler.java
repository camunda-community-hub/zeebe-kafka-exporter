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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.exporters.kafka.config.RecordConfig;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.value.WorkflowInstanceRelated;
import java.util.Objects;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * {@link RecordHandler} is responsible for testing if certain records are allowed, and if so,
 * transforming them.
 *
 * <p>Should be refactored into two for single responsibility.
 */
@SuppressWarnings("rawtypes")
public final class RecordHandler {
  private final RecordsConfig configuration;

  public RecordHandler(final @NonNull RecordsConfig configuration) {
    this.configuration = Objects.requireNonNull(configuration);
  }

  /**
   * Transforms the given {@link Record} into a Kafka {@link ProducerRecord}.
   *
   * @param record the record to transform
   * @return the transformed record
   */
  public @NonNull ProducerRecord<Long, Record> transform(final @NonNull Record record) {
    final RecordConfig config = getRecordConfig(record);
    long key = record.getPartitionId();
    if (record instanceof WorkflowInstanceRelated) {
      key = ((WorkflowInstanceRelated) record).getWorkflowInstanceKey();
    }
    return new ProducerRecord<>(config.getTopic(), key, record);
  }

  /**
   * Tests whether or not the given record is allowed, as specified by the configuration.
   *
   * @param record the record to test
   * @return true if allowed, false otherwise
   */
  public boolean test(final @NonNull Record record) {
    final RecordConfig config = getRecordConfig(record);
    return config.getAllowedTypes().contains(record.getRecordType());
  }

  private @NonNull RecordConfig getRecordConfig(final @NonNull Record record) {
    return configuration.forType(Objects.requireNonNull(record).getValueType());
  }
}
