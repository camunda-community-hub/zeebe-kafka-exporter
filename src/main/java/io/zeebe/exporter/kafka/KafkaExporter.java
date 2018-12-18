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
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;

/**
 * Implementation of a Zeebe exporter producing serialized records to a given Kafka topic.
 *
 * <p>TODO: implement another transmission strategy using transactions and see which is better
 *
 * <p>TODO: better error handling; at the moment, if a record is dropped, the exporter can never
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
  private boolean isClosed;

  @Override
  public void configure(Context context) {
    this.logger = context.getLogger();
    this.configuration = context.getConfiguration().instantiate(KafkaExporterConfiguration.class);
    this.inFlightRecords = new ArrayDeque<>(this.configuration.maxInFlightRecords);
    this.id = context.getConfiguration().getId();

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

    final ProducerRecord<Record, Record> producedRecord =
        new ProducerRecord<>(configuration.topic, record, record);
    final Future<RecordMetadata> future = producer.send(producedRecord);
    inFlightRecords.add(new KafkaExporterFuture(record.getPosition(), future));
    logger.debug(">>> Exported new record {}", record);

    if (inFlightRecords.size() >= this.configuration.maxInFlightRecords) {
      logger.debug("Too many in flight records, blocking until the next one completes...");
      awaitNextInFlightRecordCompletion();
    }
  }

  /** Blocks and waits until the next in-flight record is completed */
  private void awaitNextInFlightRecordCompletion() {
    final long latestPosition = getNextCompletedInFlightRecordPosition();
    controller.updateLastExportedRecordPosition(latestPosition);
  }

  private void closeInternal(boolean awaitInFlightRecords) {
    if (!isClosed) {
      logger.debug(
          "Closing exporter, waiting for in flight records to complete: {}", awaitInFlightRecords);
      isClosed = true;

      if (producer != null) {
        // flushes any remaining records and awaits their completion
        // todo: currently waits forever, should configure a timeout
        producer.close();
        producer = null;
      }

      if (inFlightRecords != null && !inFlightRecords.isEmpty()) {
        updatePositionBasedOnCompletedInFlightRecords(awaitInFlightRecords);
        cancelInFlightRecords();
      }
    }
  }

  private void updatePositionBasedOnCompletedInFlightRecords(boolean blockForCompletion) {
    long position = UNSET_POSITION;

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

    if (position != UNSET_POSITION) {
      logger.debug("Updating new controller position to {}", position);
      controller.updateLastExportedRecordPosition(position);
    }
  }

  private void checkCompletedInFlightRecords() {
    updatePositionBasedOnCompletedInFlightRecords(false);

    // could be null if closed in the meantime
    if (producer != null) {
      controller.scheduleTask(
          IN_FLIGHT_RECORD_CHECKER_INTERVAL, this::checkCompletedInFlightRecords);
    }
  }

  private long getNextCompletedInFlightRecordPosition() {
    final Future<Long> inFlightRecord = inFlightRecords.poll();

    if (inFlightRecord != null) {
      try {
        final long position = inFlightRecord.get();
        logger.trace("Consumed in-flight record {}", position);
        return position;
      } catch (InterruptedException e) {
        onUnrecoverableError(
            "Kafka producer thread was interrupted, most likely indicating the producer is closing",
            e);
      } catch (ExecutionException e) {
        onUnrecoverableError(
            "An error occurred while sending a record ot Kafka, most likely indicating the record was dropped",
            e);
      }
    }

    return UNSET_POSITION;
  }

  private void cancelInFlightRecords() {
    int cancelledCount = 0;

    while (!inFlightRecords.isEmpty()) {
      final Future inFlightRecord = inFlightRecords.poll();
      if (inFlightRecord != null) {
        inFlightRecord.cancel(true);
        cancelledCount++;
      }
    }

    logger.debug("Cancelled {} in flight records", cancelledCount);
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
