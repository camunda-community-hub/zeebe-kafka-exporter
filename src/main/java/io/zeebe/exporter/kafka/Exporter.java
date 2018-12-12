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
import io.zeebe.exporter.context.ScheduledTimer;
import io.zeebe.exporter.record.Record;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Exporter implements io.zeebe.exporter.spi.Exporter {
  private static final int UNSET_POSITION = -1;
  private static final Duration IN_FLIGHT_REQUEST_CHECKER_INTERVAL = Duration.ofSeconds(1);

  private Controller controller;
  private Configuration configuration;
  private Logger logger;
  private Producer<Record, Record> producer;
  private Deque<ExportFuture> inFlightRequests;
  private ScheduledTimer checkInFlightRequestTimerId;

  @Override
  public void configure(Context context) {
    this.logger = context.getLogger();
    this.configuration = context.getConfiguration().instantiate(Configuration.class);
    this.inFlightRequests = new ArrayDeque<>(this.configuration.getBatchSize());

    if (this.configuration.getTopic().isEmpty()) {
      throw new KafkaExporterException("Must configure a topic");
    }
  }

  @Override
  public void open(Controller controller) {
    this.controller = controller;
    this.producer = new KafkaProducer<>(new ExporterProperties(this.configuration));
    this.checkInFlightRequestTimerId =
        this.controller.scheduleTask(
            IN_FLIGHT_REQUEST_CHECKER_INTERVAL, this::checkCompletedInFlightRequests);
  }

  @Override
  public void close() {
    if (checkInFlightRequestTimerId != null) {
      checkInFlightRequestTimerId.cancel();
      checkInFlightRequestTimerId = null;
    }

    if (producer != null) {
      if (inFlightRequests != null && !inFlightRequests.isEmpty()) {
        awaitAllInFlightRequestCompletion();
        cancelInFlightRequests();
      }

      producer.close();
      producer = null;
    }
  }

  @Override
  public void export(Record record) {
    // The producer may be closed prematurely if an unrecoverable exception occurred, at which point
    // we ignore any further records; this way we do not block the exporter processor, and on
    // restart will reprocess all other records that we "missed" here.
    if (producer == null) {
      return;
    }

    final ProducerRecord<Record, Record> producedRecord =
        new ProducerRecord<>("topic", record, record);
    final Future<RecordMetadata> future = producer.send(producedRecord);
    inFlightRequests.add(new ExportFuture(record.getPosition(), future));

    if (inFlightRequests.size() >= this.configuration.getBatchSize()) {
      awaitNextInFlightRequestCompletion();
    }
  }

  /** Blocks and waits until the next in-flight request is completed */
  private void awaitNextInFlightRequestCompletion() {
    long latestPosition = getNextCompletedInFlightRequestPosition();
    controller.updateLastExportedRecordPosition(latestPosition);
  }

  /**
   * Blocks until in-flight requests are completed or until the first one fails, and updates the
   * position
   */
  private void awaitAllInFlightRequestCompletion() {
    updatePositionBasedOnCompletedInFlightRequests(true);
  }

  private void updatePositionBasedOnCompletedInFlightRequests(boolean blockForCompletion) {
    long position = UNSET_POSITION;

    while (!inFlightRequests.isEmpty()) {
      if (!inFlightRequests.peek().isDone() && !blockForCompletion) {
        break;
      }

      final long latestPosition = getNextCompletedInFlightRequestPosition();
      if (latestPosition != UNSET_POSITION) {
        position = latestPosition;
      } else {
        break;
      }
    }

    if (position != UNSET_POSITION) {
      controller.updateLastExportedRecordPosition(position);
    }
  }

  private void checkCompletedInFlightRequests() {
    updatePositionBasedOnCompletedInFlightRequests(false);
  }

  private long getNextCompletedInFlightRequestPosition() {
    final Future<Long> inFlightRequest = inFlightRequests.poll();

    if (inFlightRequest != null) {
      try {
        return inFlightRequest.get();
      } catch (InterruptedException e) {
        onUnrecoverableError(
            "Kafka producer thread was interrupted, most likely indicating the producer is closing",
            e);
      } catch (ExecutionException e) {
        onUnrecoverableError(
            "An exception occurred while awaiting the completion of an in flight export request",
            e);
      }
    }

    return -1;
  }

  private void cancelInFlightRequests() {
    while (!inFlightRequests.isEmpty()) {
      final Future inFlightRequest = inFlightRequests.poll();
      if (inFlightRequest != null) {
        inFlightRequest.cancel(true);
      }
    }
  }

  private void onUnrecoverableError(String details, Exception e) {
    final String message =
        String.format(
            "Unrecoverable error occurred: %s; closing producer, all subsequent records will be ignored.",
            details);
    logger.error(message, e);
    close();
  }
}
