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
import java.util.LinkedList;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.TimeoutException;
import org.slf4j.Logger;

/**
 * An implementation of {@link RecordBatch} which uses Kafka transactions to guarantee the atomicity
 * of the flush operation. When records are added, it will first add them to a linked list before
 * immediately forwarding them to the producer. If there was no transaction yet, it will be started
 * before. On flush, the transaction is committed.
 *
 * <p>NOTE: while atomicity could still be guaranteed without transactions, they make the whole
 * error handling much simpler. I do realize that we cannot use the exactly-once semantics due to
 * Zeebe's own at-least-once semantics, but it still seems useful to simplify error handling.
 *
 * <p>NOTE: whenever an error occurs, if it is recoverable, it will be logged and the batch remains
 * as is - the operation will be retried either by adding a new record or by attempting to flush the
 * batch externally. If it's unrecoverable, the current producer is closed, the state is reset
 * (minus the linked list which remains the same so we can retry the records), and on the next add
 * or flush operation, the whole batch is retried.
 *
 * <p>NOTE: when adding a record to a full batch, it will attempt to flush the batch, blocking up to
 * {@link io.zeebe.exporters.kafka.config.raw.RawProducerConfig#maxBlockingTimeoutMs} milliseconds.
 * If it flushed successfully, then the record will be added and operations will resume as normal.
 * If it failed to flush, then the error will bubble up wrapped in a {@link
 * FullRecordBatchException}.
 *
 * <p>NOTE: when using this type of batch, make sure your consumers use "read_committed" as
 * isolation level, otherwise they may see uncommitted records. This isn't too big of a deal as
 * these records are anyway committed on the Zeebe side, but they may show up as duplicates.
 */
final class BoundedTransactionalRecordBatch implements RecordBatch {
  private final LinkedList<ProducerRecord<RecordId, byte[]>> records = new LinkedList<>();

  private final KafkaProducerFactory producerFactory;
  private final ProducerConfig config;
  private final String producerId;
  private final int maxBatchSize;
  private final LongConsumer onFlushCallback;
  private final Logger logger;

  private Producer<RecordId, byte[]> producer;
  private boolean producerInitialized = false;
  private boolean transactionBegan = false;
  private int nextSendIndex = 0;

  public BoundedTransactionalRecordBatch(
      final ProducerConfig config,
      final int maxBatchSize,
      final LongConsumer onFlushCallback,
      final Logger logger,
      final KafkaProducerFactory producerFactory) {
    this(
        config,
        maxBatchSize,
        onFlushCallback,
        logger,
        producerFactory,
        UUID.randomUUID().toString());
  }

  public BoundedTransactionalRecordBatch(
      final ProducerConfig config,
      final int maxBatchSize,
      final LongConsumer onFlushCallback,
      final Logger logger,
      final KafkaProducerFactory producerFactory,
      final String producerId) {
    this.config = Objects.requireNonNull(config);
    this.maxBatchSize = maxBatchSize;
    this.onFlushCallback = Objects.requireNonNull(onFlushCallback);
    this.logger = Objects.requireNonNull(logger);
    this.producerFactory = Objects.requireNonNull(producerFactory);
    this.producerId = Objects.requireNonNull(producerId);
  }

  @Override
  public void add(final ProducerRecord<RecordId, byte[]> record) throws FullRecordBatchException {
    if (records.size() >= maxBatchSize) {
      try {
        flushBatch();
      } catch (final TimeoutException | InterruptException e) {
        throw new FullRecordBatchException(maxBatchSize, e);
      } catch (final Exception e) {
        close();
        throw new FullRecordBatchException(maxBatchSize, e);
      }
    }

    records.add(record);

    try {
      sendUnsentRecords();
    } catch (final TimeoutException | InterruptException e) {
      logger.debug(
          "Timed out or interrupted while sending unsent records, will be retried later", e);
    } catch (final Exception e) {
      logger.warn("Failed to send unsent record, will be retried later with a new producer", e);
      close();
    }
  }

  @Override
  public void flush() {
    if (records.isEmpty()) {
      logger.trace("Skipping batch commit as there are no records in the batch");
      return;
    }

    logger.trace(
        "Committing {} from the current batch, up to position {}",
        records.size(),
        records.getLast().key().getPosition());

    try {
      flushBatch();
    } catch (final TimeoutException | InterruptException e) {
      logger.debug("Timed out or interrupted while committing, will be retried later", e);
    } catch (final Exception e) {
      logger.warn("Non-recoverable error occurred while committing, retrying with new producer", e);
      close();
    }
  }

  @Override
  public void close() {
    if (producer == null) {
      return;
    }

    final var closeTimeout = config.getCloseTimeout();
    logger.debug("Closing producer with timeout {}", closeTimeout);

    try {
      producer.close(closeTimeout);
    } catch (final Exception e) {
      logger.warn(
          "Failed to gracefully close Kafka exporter; this is most likely fine, but may cause "
              + "resource to leaks. Investigate if it keeps repeating itself.",
          e);
    }

    producer = null;
    producerInitialized = false;
    transactionBegan = false;
    nextSendIndex = 0;
    // the records' list is not cleared on purpose, so that we can later try it
  }

  private void flushBatch() throws KafkaException, IllegalStateException {
    sendUnsentRecords();

    final var commitPosition = records.getLast().key().getPosition();
    commitTransaction();
    onFlushCallback.accept(commitPosition);
  }

  private void commitTransaction() {
    if (!transactionBegan) {
      throw new IllegalStateException(
          "Expected to be in transaction, but no transaction is in flight");
    }

    producer.commitTransaction();
    transactionBegan = false;
    records.clear();
    nextSendIndex = 0;
  }

  private void sendUnsentRecords() {
    final var unsentRecords = Math.max(0, records.size() - nextSendIndex);
    logger.trace("Sending {} remaining unsent records from the current batch", unsentRecords);

    ensureWithinTransaction();

    while (nextSendIndex < records.size()) {
      final var record = records.get(nextSendIndex);
      producer.send(record);
      logger.trace("Sent record {}", record);
      nextSendIndex++;
    }
  }

  private void ensureProducer() {
    if (producer != null) {
      return;
    }

    producer = producerFactory.newProducer(config, producerId);
    logger.trace("Created new producer");
  }

  private void ensureProducerInitialized() {
    ensureProducer();

    if (!producerInitialized) {
      producer.initTransactions();
      producerInitialized = true;
      logger.trace("Initialized producer for transactions");
    }
  }

  private void ensureWithinTransaction() {
    ensureProducerInitialized();

    if (!transactionBegan) {
      producer.beginTransaction();
      transactionBegan = true;
      logger.trace("Began new producer transaction");
    }
  }
}
