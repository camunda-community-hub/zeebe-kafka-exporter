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
package io.zeebe.exporters.kafka;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.exporter.api.Exporter;
import io.zeebe.exporter.api.context.Context;
import io.zeebe.exporter.api.context.Controller;
import io.zeebe.exporters.kafka.batch.BatchedRecord;
import io.zeebe.exporters.kafka.batch.BatchedRecordException;
import io.zeebe.exporters.kafka.batch.RecordBatch;
import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.config.parser.ConfigParser;
import io.zeebe.exporters.kafka.config.parser.RawConfigParser;
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.producer.DefaultKafkaProducerFactory;
import io.zeebe.exporters.kafka.producer.KafkaProducerFactory;
import io.zeebe.exporters.kafka.record.KafkaRecordFilter;
import io.zeebe.exporters.kafka.record.RecordHandler;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.serde.RecordSerializer;
import io.zeebe.protocol.record.Record;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;

/** Implementation of a Zeebe exporter producing serialized records to a given Kafka topic. */
public class KafkaExporter implements Exporter {
  static final Duration IN_FLIGHT_RECORD_CHECKER_INTERVAL = Duration.ofSeconds(1);
  private static final int UNSET_POSITION = -1;

  private final KafkaProducerFactory producerFactory;
  private final ConfigParser<RawConfig, Config> configParser;

  private boolean isClosed = true;
  private String id;
  private Controller controller;
  private Logger logger;
  private Producer<RecordId, byte[]> producer;
  private Config config;
  private RecordHandler recordHandler;
  private RecordBatch recordBatch;
  private long latestExportedPosition = UNSET_POSITION;

  public KafkaExporter() {
    this(new DefaultKafkaProducerFactory(), new RawConfigParser());
  }

  public KafkaExporter(
      final @NonNull KafkaProducerFactory producerFactory,
      final @NonNull ConfigParser<RawConfig, Config> configParser) {
    this.producerFactory = Objects.requireNonNull(producerFactory);
    this.configParser = Objects.requireNonNull(configParser);
  }

  @Override
  public void configure(final Context context) {
    this.logger = context.getLogger();
    this.id = context.getConfiguration().getId();

    final RawConfig rawConfig = context.getConfiguration().instantiate(RawConfig.class);
    this.config = this.configParser.parse(rawConfig);
    this.recordHandler = new RecordHandler(this.config.getRecords(), newSerializer());

    context.setFilter(new KafkaRecordFilter(this.config.getRecords()));
    this.logger.debug("Configured exporter {}", this.id);
  }

  @Override
  public void open(final Controller controller) {
    this.controller = controller;
    this.isClosed = false;
    this.producer = this.producerFactory.newProducer(this.config);
    this.recordBatch = new RecordBatch(this.config.getMaxBatchSize());
    this.controller.scheduleTask(
        this.config.getInFlightRecordCheckInterval(), this::checkCompletedInFlightRequests);

    this.logger.debug("Opened exporter {}", this.id);
  }

  @Override
  public void close() {
    if (isClosed) {
      return;
    }

    isClosed = true;
    recordBatch.cancel();
    checkCompletedInFlightRequests();
    recordBatch.clear();
    closeProducer();

    logger.debug("Closed exporter {}", id);
  }

  @Override
  public void export(final Record record) {
    if (isClosed) {
      logger.warn("Expected to export {}, but the exporter is already closed", record);
      return;
    }

    if (!recordHandler.test(record)) {
      logger.trace("Ignoring record {}", record);
      return;
    }

    if (recordBatch.isFull()) {
      // this will await completion of the record and update the position, OR an exception may be
      // thrown which will cause us to retry - either way, if we pass this then we can assume
      // there's space in the batch now
      logger.trace(
          "Too many in flight records, blocking at most {} until at least one completes...",
          Duration.ofSeconds(1));
      recordBatch.consume(this::updatePosition);
    }

    batchRecord(record);
    logger.trace("Batched record {}", record);
  }

  /* assumes it is called strictly as a scheduled task or during close */
  private void checkCompletedInFlightRequests() {
    try {
      recordBatch.consumeCompleted(this::updatePosition);
    } catch (final BatchedRecordException e) {
      handleBatchedRecordException(e);
    }

    if (latestExportedPosition != UNSET_POSITION) {
      controller.updateLastExportedRecordPosition(latestExportedPosition);
    }

    if (!isClosed) {
      controller.scheduleTask(
          IN_FLIGHT_RECORD_CHECKER_INTERVAL, this::checkCompletedInFlightRequests);
    }
  }

  @SuppressWarnings("rawtypes")
  private Serializer<Record> newSerializer() {
    final Serializer<Record> serializer = new RecordSerializer();
    serializer.configure(config.getProducer().getConfig(), false);

    return serializer;
  }

  @SuppressWarnings("rawtypes")
  private void batchRecord(final Record record) {
    final ProducerRecord<RecordId, byte[]> producerRecord = recordHandler.transform(record);
    final Future<RecordMetadata> request = producer.send(producerRecord);

    recordBatch.add(producerRecord, request);
  }

  private void updatePosition(final BatchedRecord record) {
    try {
      if (!record.wasExported()) {
        record.awaitCompletion(config.getMaxBlockingTimeout());
      }

      if (record.wasCancelled()) {
        logger.debug("Skipping already cancelled record as we cannot guarantee it was exported");
        return;
      }

      latestExportedPosition = record.getRecord().key().getPosition();
    } catch (final ExecutionException e) {
      // rethrow and let this be handled where we're expecting it; by doing this we can also ensure
      // the record is not removed from the batch yet
      throw new BatchedRecordException(record, e);
    } catch (final CancellationException e) {
      // if this occurs while we're waiting for the result, then we can safely expect that it was
      // cancelled in the context of this exporter, so there's nothing to do here
      logger.debug("Record {} was cancelled while awaiting result", record, e);
    } catch (final InterruptedException e) { // NOSONAR - InterruptException will re-interrupt
      // being interrupted while awaiting completion of the request most likely means the exporter
      // thread is shutting down, at which point we can expect to be closed; so simply rethrow this
      // exception
      throw new InterruptException(e);
    } catch (final TimeoutException e) {
      throw new BlockingRequestTimeoutException(record, config.getMaxBlockingTimeout(), e);
    }
  }

  private void closeProducer() {
    if (producer != null) {
      final Duration closeTimeout = config.getProducer().getCloseTimeout();
      logger.debug("Closing producer with timeout {}", closeTimeout);
      producer.close(closeTimeout);
      producer = null;
    }
  }

  /**
   * A {@link BatchedRecordException} is thrown when attempting to check the result of a record that
   * was sent by the producer via {@link Producer#send(ProducerRecord)}, and an error occurred.
   *
   * <p>There are two possible classes of errors: recoverable and unrecoverable ones. These are
   * outlined in {@link org.apache.kafka.clients.producer.Callback}. In both cases, we have to
   * cancel all in flight records, as we don't know how the producer batched internally - to
   * preserve logical ordering, we then cancel all records, and retry them.
   *
   * <p>Additionally, in the case of an unrecoverable error, we recreate the producer, which may
   * help solve some issues. However, some errors are plain and simply non recoverable - an
   * authentication error will not be solved without a configuration change.
   *
   * @param e the exception thrown
   */
  private void handleBatchedRecordException(final BatchedRecordException e) {
    logger.warn(
        "Cancelling all in flight records after {} and retrying them due to an exception thrown by the underlying producer",
        e.getRecord(),
        e);

    if (!e.isRecoverable()) {
      logger.debug("Recreating producer due to an unrecoverable exception", e);
      closeProducer();
      producer = producerFactory.newProducer(config);
    }

    for (final BatchedRecord batchedRecord : recordBatch) {
      batchedRecord.cancel();

      final Future<RecordMetadata> request = producer.send(batchedRecord.getRecord());
      batchedRecord.retry(request);
    }
  }
}
