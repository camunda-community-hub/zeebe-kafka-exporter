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

import io.zeebe.exporter.context.Context;
import io.zeebe.exporter.context.Controller;
import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.spi.Exporter;
import io.zeebe.util.DurationUtil;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.InterruptException;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of a Zeebe exporter producing serialized records to a given Kafka topic.
 *
 * <p>TODO: implement another transmission strategy using transactions and see which is better
 *
 * <p>TODO: when exporter closed unexpectedly, what should happen?
 */
public class KafkaExporter implements Exporter {
  static final Duration IN_FLIGHT_RECORD_CHECKER_INTERVAL = Duration.ofSeconds(1);
  private static final int UNSET_POSITION = -1;

  private String id;
  private Controller controller;
  private KafkaExporterConfiguration configuration;
  private Logger logger;
  private Producer<Record, Record> producer;
  private Deque<KafkaExporterFuture> inFlightRecords;
  private boolean isClosed = true;

  private Duration awaitInFlightRecordTimeout;
  private Duration closeProducerTimeout;

  @Override
  public void configure(Context context) {
    this.logger = context.getLogger();
    this.configuration = context.getConfiguration().instantiate(KafkaExporterConfiguration.class);
    this.inFlightRecords = new ArrayDeque<>(this.configuration.maxInFlightRecords);
    this.id = context.getConfiguration().getId();
    this.awaitInFlightRecordTimeout =
        DurationUtil.parse(this.configuration.awaitInFlightRecordTimeout);
    this.closeProducerTimeout = DurationUtil.parse(this.configuration.producer.closeTimeout);

    if (this.configuration.topic == null || this.configuration.topic.isEmpty()) {
      throw new KafkaExporterException("Must configure a topic");
    }

    logger.debug("Configured exporter {} with {}", this.id, this.configuration);
  }

  @Override
  public void open(Controller controller) {
    this.controller = controller;
    this.controller.scheduleTask(
        IN_FLIGHT_RECORD_CHECKER_INTERVAL, this::checkCompletedInFlightRecords);
    this.producer = this.configuration.newProducer(this.id);
    this.isClosed = false;

    logger.debug("Opened exporter {}", this.id);
  }

  @Override
  public void close() {
    logger.debug("Exporter close requested");
    closeInternal(true);
  }

  @Override
  public void export(Record record) {
    // The producer may be closed prematurely if an unrecoverable exception occurred, at which point
    // we ignore any further records; this way we do not block the exporter processor, and on
    // restart will reprocess all other records that we "missed" here.
    if (producer == null) {
      logger.trace("Already closed internally, probably due to error, skipping record {}", record);
      return;
    }

    if (inFlightRecords.size() >= this.configuration.maxInFlightRecords) {
      logger.debug("Too many in flight records, blocking until the next one completes...");
      awaitNextInFlightRecordCompletion();
    }

    final ProducerRecord<Record, Record> producedRecord =
        new ProducerRecord<>(configuration.topic, record, record);
    final Future<RecordMetadata> future = producer.send(producedRecord);
    inFlightRecords.add(new KafkaExporterFuture(record.getPosition(), future));
    logger.debug(">>> Exported new record {}", record);
  }

  /** Blocks and waits until the next in-flight record is completed */
  private void awaitNextInFlightRecordCompletion() {
    final long latestPosition = getNextCompletedInFlightRecordPosition();
    updatePosition(latestPosition);
  }

  private void closeInternal(boolean awaitInFlightRecords) {
    if (!isClosed) {
      logger.debug(
          "Closing exporter, waiting for in flight records to complete: {}", awaitInFlightRecords);
      isClosed = true;

      if (producer != null) {
        try {
          closeProducer();
        } catch (InterruptException e) {
          // thread interrupted, most likely shutting down, so don't block later down the line
          awaitInFlightRecords = false;
        }
      }

      if (inFlightRecords != null && !inFlightRecords.isEmpty()) {
        updatePositionBasedOnCompletedInFlightRecords(awaitInFlightRecords);
        dropInFlightRecords();
      }
    }
  }

  // flushes any remaining records and awaits their completion
  private void closeProducer() {
    logger.debug("Closing producer with timeout {}", closeProducerTimeout);
    producer.close(closeProducerTimeout.toMillis(), TimeUnit.MILLISECONDS);
    producer = null;
  }

  private void updatePositionBasedOnCompletedInFlightRecords(boolean blockForCompletion) {
    long position = UNSET_POSITION;

    try {
      while (!inFlightRecords.isEmpty()) {
        if (!inFlightRecords.peek().isDone() && !blockForCompletion) {
          break;
        }

        final long latestPosition = getNextCompletedInFlightRecordPosition();
        if (latestPosition != UNSET_POSITION) {
          position = latestPosition;
        } else {
          break;
        }
      }
    } finally {
      // try updating with whatever position we managed to get
      updatePosition(position);
    }
  }

  private void updatePosition(long position) {
    if (position != UNSET_POSITION) {
      logger.debug("Updating new controller position to {}", position);
      controller.updateLastExportedRecordPosition(position);
    }
  }

  /* assumes it is called strictly as a scheduled task */
  private void checkCompletedInFlightRecords() {
    updatePositionBasedOnCompletedInFlightRecords(false);

    if (!isClosed) {
      controller.scheduleTask(
          IN_FLIGHT_RECORD_CHECKER_INTERVAL, this::checkCompletedInFlightRecords);
    }
  }

  private long getNextCompletedInFlightRecordPosition() {
    final KafkaExporterFuture inFlightRecord = inFlightRecords.peek(); // in case of error

    if (inFlightRecord != null) {
      try {
        final long position =
            inFlightRecord.get(awaitInFlightRecordTimeout.toMillis(), TimeUnit.MILLISECONDS);
        inFlightRecords.remove();
        logger.trace("Consumed in-flight record {}", position);
        return position;
      } catch (TimeoutException e) {
        throw new KafkaExporterException(
            String.format(
                "Timed out after %s awaiting completion of record", awaitInFlightRecordTimeout),
            e);
      } catch (InterruptedException e) {
        onUnrecoverableError(
            "Kafka producer thread was interrupted, most likely indicating the producer is closing",
            e);
      } catch (ExecutionException e) {
        /* Kafka reports the most likely reason for this error is that the record was
         * dropped (so not retried?), at which point the exporter is broken and can never
         * recover since we lost the initial record (except by restarting the broker).
         * At the moment, the chosen strategy is too block and loop forever, but that means
         * blocking the stream processor forever and ever...
         */
        throw new KafkaExporterException(
            "An error occurred while sending a record ot Kafka, most likely indicating the record was dropped",
            e);
      }
    }

    return UNSET_POSITION;
  }

  private void dropInFlightRecords() {
    int droppedCount = 0;

    while (!inFlightRecords.isEmpty()) {
      final Future inFlightRecord = inFlightRecords.poll();
      if (inFlightRecord != null) {
        inFlightRecord.cancel(true);
        droppedCount++;
      }
    }

    logger.debug("Dropped {} in flight records", droppedCount);
  }

  private void onUnrecoverableError(String details, Exception e) {
    final String message =
        String.format(
            "Unrecoverable error occurred: %s; closing producer, all subsequent records will be ignored.",
            details);
    logger.error(message, e);
    closeInternal(false);
  }
}
