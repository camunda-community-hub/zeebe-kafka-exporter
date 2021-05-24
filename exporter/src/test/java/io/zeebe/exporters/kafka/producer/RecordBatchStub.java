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

import io.zeebe.exporters.kafka.config.ProducerConfig;
import io.zeebe.exporters.kafka.record.FullRecordBatchException;
import io.zeebe.exporters.kafka.serde.RecordId;
import java.nio.BufferOverflowException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;

public final class RecordBatchStub implements RecordBatch {
  public RuntimeException flushException;

  private final ProducerConfig config;
  private final int maxBatchSize;
  private final LongConsumer onFlushCallback;
  private final Logger logger;

  private final LinkedList<ProducerRecord<RecordId, byte[]>> flushedRecords = new LinkedList<>();

  private final LinkedList<ProducerRecord<RecordId, byte[]>> pendingRecords = new LinkedList<>();

  private boolean closed = false;

  public RecordBatchStub(
      final ProducerConfig config,
      final int maxBatchSize,
      final LongConsumer onFlushCallback,
      final Logger logger) {
    this.config = Objects.requireNonNull(config);
    this.maxBatchSize = maxBatchSize;
    this.onFlushCallback = Objects.requireNonNull(onFlushCallback);
    this.logger = Objects.requireNonNull(logger);
  }

  @Override
  public void add(final ProducerRecord<RecordId, byte[]> record) throws FullRecordBatchException {
    if (pendingRecords.size() >= maxBatchSize) {
      throw new FullRecordBatchException(maxBatchSize, new BufferOverflowException());
    }

    pendingRecords.add(record);
  }

  @Override
  public void flush() {
    if (flushException != null) {
      throw flushException;
    }

    flushedRecords.addAll(pendingRecords);
    pendingRecords.clear();

    if (!flushedRecords.isEmpty()) {
      onFlushCallback.accept(flushedRecords.getLast().key().getPosition());
    }
  }

  @Override
  public void close() {
    closed = true;
  }

  public List<ProducerRecord<RecordId, byte[]>> getFlushedRecords() {
    return flushedRecords;
  }

  public List<ProducerRecord<RecordId, byte[]>> getPendingRecords() {
    return pendingRecords;
  }

  public boolean isClosed() {
    return closed;
  }

  public static class Factory implements RecordBatchFactory {
    public RecordBatchStub stub;

    @Override
    public RecordBatch newRecordBatch(
        final ProducerConfig config,
        final int maxBatchSize,
        final LongConsumer onFlushCallback,
        final Logger logger) {
      if (stub == null) {
        stub = new RecordBatchStub(config, maxBatchSize, onFlushCallback, logger);
      }

      return stub;
    }
  }
}
