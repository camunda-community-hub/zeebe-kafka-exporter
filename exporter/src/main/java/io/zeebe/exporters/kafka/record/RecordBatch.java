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
import io.zeebe.exporters.kafka.serde.RecordId;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.kafka.clients.producer.ProducerRecord;

public final class RecordBatch implements Iterable<ProducerRecord<RecordId, byte[]>> {

  /**
   * Attempts to estimate the size of a {@link RecordId} in bytes. This was obtained by serializing
   * one using {@link io.zeebe.exporters.kafka.serde.RecordIdSerializer} with maximum integer and
   * long values for both properties.
   */
  private static final int ESTIMATED_SERIALIZED_RECORD_ID_SIZE_IN_BYTES = 57;

  private final LinkedList<ProducerRecord<RecordId, byte[]>> records;
  private final long maxSizeInBytes;

  private long sizeInBytes;
  private long highestPosition;

  public RecordBatch(final long maxSizeInBytes) {
    this.maxSizeInBytes = maxSizeInBytes;
    this.records = new LinkedList<>();

    this.sizeInBytes = 0L;
  }

  public void add(final @NonNull ProducerRecord<RecordId, byte[]> record) {
    records.add(record);
    highestPosition = Math.max(highestPosition, record.key().getPosition());
    sizeInBytes += estimateSerializedRecordSize(record);
  }

  public boolean isFull() {
    return sizeInBytes >= maxSizeInBytes;
  }

  public void clear() {
    records.clear();
    sizeInBytes = 0;
    highestPosition = -1;
  }

  public boolean isEmpty() {
    return records.isEmpty();
  }

  public long getHighestPosition() {
    return highestPosition;
  }

  public int recordsCount() {
    return records.size();
  }

  @Override
  public Iterator<ProducerRecord<RecordId, byte[]>> iterator() {
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
