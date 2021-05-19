package io.zeebe.exporters.kafka.producer;

import io.zeebe.exporters.kafka.config.ProducerConfig;
import io.zeebe.exporters.kafka.record.FullRecordBatchException;
import io.zeebe.exporters.kafka.serde.RecordId;
import java.util.LinkedList;
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
public final class TransactionBoundedRecordBatch implements RecordBatch {
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

  public TransactionBoundedRecordBatch(
      final KafkaProducerFactory producerFactory,
      final ProducerConfig config,
      final String producerId,
      final int maxBatchSize,
      final LongConsumer onFlushCallback,
      final Logger logger) {
    this.producerFactory = producerFactory;
    this.config = config;
    this.producerId = producerId;
    this.maxBatchSize = maxBatchSize;
    this.onFlushCallback = onFlushCallback;
    this.logger = logger;
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
    assert transactionBegan : "should not commit if no transaction is ongoing";

    producer.commitTransaction();
    transactionBegan = false;
    records.clear();
    nextSendIndex = 0;
  }

  private void sendUnsentRecords() {
    final var unsentRecords = Math.max(0, records.size() - nextSendIndex);
    logger.trace("Sending {} remaining unsent records from the current batch", unsentRecords);

    ensureProducerInitialized();
    ensureWithinTransaction();

    while (nextSendIndex < records.size()) {
      final var record = records.get(nextSendIndex);
      producer.send(record);
      logger.trace("Sent record {}", record);
      nextSendIndex++;
    }
  }

  private void ensureProducerInitialized() {
    if (producer == null) {
      producer = producerFactory.newProducer(config, producerId);
      logger.trace("Created new producer");
    }

    if (!producerInitialized) {
      producer.initTransactions();
      producerInitialized = true;
      logger.trace("Initialized producer for transactions");
    }
  }

  private void ensureWithinTransaction() {
    if (!transactionBegan) {
      producer.beginTransaction();
      transactionBegan = true;
      logger.trace("Began new producer transaction");
    }
  }
}
