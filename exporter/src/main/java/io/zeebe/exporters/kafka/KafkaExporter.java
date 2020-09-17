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
import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.config.parser.ConfigParser;
import io.zeebe.exporters.kafka.config.parser.RawConfigParser;
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.producer.DefaultKafkaProducerFactory;
import io.zeebe.exporters.kafka.producer.KafkaProducerFactory;
import io.zeebe.exporters.kafka.record.KafkaRecordFilter;
import io.zeebe.exporters.kafka.record.RecordHandler;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.util.Request;
import io.zeebe.exporters.kafka.util.RequestQueue;
import io.zeebe.protocol.record.Record;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
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

  @SuppressWarnings("rawtypes")
  private Producer<RecordId, Record> producer;

  private Config config;
  private RecordHandler recordHandler;
  private RequestQueue requests;
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
    this.recordHandler = new RecordHandler(this.config.getRecords());

    context.setFilter(new KafkaRecordFilter(this.config.getRecords()));
    this.logger.debug("Configured exporter {}", this.id);
  }

  @Override
  public void open(final Controller controller) {
    this.controller = controller;
    this.isClosed = false;
    this.requests = new RequestQueue(this.config.getMaxInFlightRecords());
    this.producer = this.producerFactory.newProducer(this.config);
    this.controller.scheduleTask(
        this.config.getInFlightRecordCheckInterval(), this::checkCompletedInFlightRequests);

    this.logger.debug("Opened exporter {}", this.id);
  }

  @Override
  public void close() {
    closeInternal();
    checkCompletedInFlightRequests();
    requests.cancelAll();

    logger.debug("Closed exporter {}", id);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void export(final Record record) {
    // The producer may be closed prematurely if an unrecoverable exception occurred, at which point
    // we ignore any further records; this way we do not block the exporter processor, and on
    // restart will reprocess all other records that we "missed" here.
    if (producer == null) {
      requests.cancelAll();
      logger.debug("Exporter {} was prematurely closed earlier; skipping record {}", id, record);
      return;
    }

    if (recordHandler.test(record)) {
      final ProducerRecord<RecordId, Record> kafkaRecord = recordHandler.transform(record);
      final Future<RecordMetadata> future = producer.send(kafkaRecord);
      final Request request = new Request(record.getPosition(), future);

      while (!requests.offer(request)) {
        logger.trace("Too many in flight records, blocking until at least one completes...");
        requests.consume(this::updatePosition);
      }

      logger.trace("Exported record {}", record);
    }
  }

  /* assumes it is called strictly as a scheduled task */
  private void checkCompletedInFlightRequests() {
    requests.consumeCompleted(this::updatePosition);
    if (latestExportedPosition != UNSET_POSITION) {
      controller.updateLastExportedRecordPosition(latestExportedPosition);
    }

    if (!isClosed) {
      controller.scheduleTask(
          IN_FLIGHT_RECORD_CHECKER_INTERVAL, this::checkCompletedInFlightRequests);
    }
  }

  private void updatePosition(final Request request) {
    try {
      latestExportedPosition = request.get();
    } catch (final CancellationException e) {
      logger.error(
          "In flight record was cancelled prematurely, will stop exporting to prevent missing records",
          e);
      closeInternal();
    } catch (final ExecutionException e) {
      logger.error(
          "Failed to ensure record was sent to Kafka, will stop exporting to prevent missing records",
          e);
      closeInternal();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.debug("Interrupted while updating the last exported position, closing the exporter");
      closeInternal();
    }
  }

  private void closeInternal() {
    if (!isClosed) {
      isClosed = true;

      if (producer != null) {
        final Duration closeTimeout = config.getProducer().getCloseTimeout();
        logger.debug("Closing producer with timeout {}", closeTimeout);
        producer.close(closeTimeout);
        producer = null;
      }
    }
  }
}
