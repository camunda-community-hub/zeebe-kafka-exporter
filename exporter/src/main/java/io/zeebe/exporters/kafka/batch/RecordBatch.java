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
package io.zeebe.exporters.kafka.batch;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.exporters.kafka.serde.RecordId;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public final class RecordBatch implements Iterable<BatchedRecord> {

  /**
   * Attempts to estimate the size of a {@link RecordId} in bytes. This was obtained by serializing
   * one using {@link io.zeebe.exporters.kafka.serde.RecordIdSerializer} with maximum integer and
   * long values for both properties.
   */
  private static final int ESTIMATED_SERIALIZED_RECORD_ID_SIZE_IN_BYTES = 57;

  private final LinkedList<BatchedRecord> records;
  private final long maxSizeInBytes;

  private long sizeInBytes;

  public RecordBatch(final long maxSizeInBytes) {
    this.maxSizeInBytes = maxSizeInBytes;
    this.records = new LinkedList<>();

    this.sizeInBytes = 0L;
  }

  public void add(
      final @NonNull ProducerRecord<RecordId, byte[]> record,
      final @NonNull Future<RecordMetadata> request) {
    final BatchedRecord batchedRecord = new BatchedRecord(record, request);
    records.add(batchedRecord);
    sizeInBytes += estimateSerializedRecordSize(record);
  }

  public boolean isFull() {
    return sizeInBytes >= maxSizeInBytes;
  }

  public void clear() {
    records.clear();
    sizeInBytes = 0;
  }

  public void cancel() {
    records.forEach(BatchedRecord::cancel);
  }

  public void consume(final @NonNull Consumer<BatchedRecord> consumer) {
    Objects.requireNonNull(consumer);

    final BatchedRecord batchedRecord = records.peek();
    if (batchedRecord != null) {
      consumer.accept(batchedRecord);
      records.remove();
    }
  }

  public void consumeCompleted(final @NonNull Consumer<BatchedRecord> consumer) {
    Objects.requireNonNull(consumer);
    BatchedRecord batchedRecord = records.peek();

    while (batchedRecord != null && batchedRecord.wasExported()) {
      consumer.accept(batchedRecord);
      records.remove();
      batchedRecord = records.peek();
    }
  }

  @NonNull
  @Override
  public Iterator<BatchedRecord> iterator() {
    return records.iterator();
  }

  /**
   * Every {@link ProducerRecord} will be at least the size of the topic serialized, the size of the
   * key serialized, and the size of the value serialized as well. On top of this it will also
   * contain the size of the headers, but that is not counted here. This means the size here is a
   * very soft lower bound, but it gives us an idea more or less how much it will use in memory.
   *
   * @param record the record to estimate
   * @return an estimated size in bytes the record will take in memory
   */
  private int estimateSerializedRecordSize(@NonNull final ProducerRecord<RecordId, byte[]> record) {
    final int estimatedTopicSizeInBytes = record.topic().getBytes(StandardCharsets.UTF_8).length;
    final int estimatedRecordSizeInBytes = record.value().length;

    return ESTIMATED_SERIALIZED_RECORD_ID_SIZE_IN_BYTES
        + estimatedTopicSizeInBytes
        + estimatedRecordSizeInBytes;
  }
}
