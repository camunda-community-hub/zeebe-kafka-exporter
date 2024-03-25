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
package at.phactum.zeebe.exporters.kafka;

import at.phactum.zeebe.exporters.kafka.config.Config;
import at.phactum.zeebe.exporters.kafka.config.parser.ConfigParser;
import at.phactum.zeebe.exporters.kafka.config.parser.RawConfigParser;
import at.phactum.zeebe.exporters.kafka.config.raw.RawConfig;
import at.phactum.zeebe.exporters.kafka.producer.RecordBatch;
import at.phactum.zeebe.exporters.kafka.producer.RecordBatchFactory;
import at.phactum.zeebe.exporters.kafka.record.KafkaRecordFilter;
import at.phactum.zeebe.exporters.kafka.record.RecordSerializer;
import io.camunda.zeebe.exporter.api.Exporter;
import io.camunda.zeebe.exporter.api.context.Context;
import io.camunda.zeebe.exporter.api.context.Controller;
import io.camunda.zeebe.exporter.api.context.ScheduledTask;
import io.camunda.zeebe.protocol.record.Record;
import at.phactum.zeebe.exporters.kafka.record.RecordHandler;

import java.util.Objects;
import org.slf4j.Logger;

/** Implementation of a Zeebe exporter producing serialized records to a given Kafka topic. */
public final class KafkaExporter implements Exporter {
  private final RecordBatchFactory recordBatchFactory;
  private final ConfigParser<RawConfig, Config> configParser;

  private Controller controller;
  private Logger logger;
  private Config config;
  private RecordHandler recordHandler;
  private ScheduledTask flushTask;
  private RecordBatch recordBatch;

  // the constructor is used by the Zeebe broker to instantiate it
  @SuppressWarnings("unused")
  public KafkaExporter() {
    this(RecordBatchFactory.defaultFactory(), new RawConfigParser());
  }

  public KafkaExporter(
      final RecordBatchFactory recordBatchFactory,
      final ConfigParser<RawConfig, Config> configParser) {
    this.recordBatchFactory = Objects.requireNonNull(recordBatchFactory);
    this.configParser = Objects.requireNonNull(configParser);
  }

  @Override
  public void configure(final Context context) {
    logger = Objects.requireNonNull(context.getLogger());

    final var rawConfig =
        Objects.requireNonNull(context.getConfiguration().instantiate(RawConfig.class));
    config = configParser.parse(rawConfig);

    final var serializer = new RecordSerializer();
    serializer.configure(config.getProducer().getConfig(), false);
    recordHandler = new RecordHandler(config.getRecords(), serializer);

    context.setFilter(new KafkaRecordFilter(config.getRecords()));

    if (logger.isDebugEnabled()) {
      logger.debug("Configured Kafka exporter: {}", config);
    } else {
      logger.info("Configured Kafka exporter");
    }
  }

  @Override
  public void open(final Controller controller) {
    this.controller = controller;
    recordBatch =
        recordBatchFactory.newRecordBatch(
            config.getProducer(), config.getMaxBatchSize(), this::updatePosition, logger);

    scheduleFlushBatchTask();

    if (logger.isDebugEnabled()) {
      logger.debug("Opened Kafka exporter with configuration: {}", config);
    } else {
      logger.info("Opened Kafka exporter");
    }
  }

  @Override
  public void close() {
    if (flushTask != null) {
      flushTask.cancel();
    }

    if (recordBatch != null) {
      recordBatch.flush();
      recordBatch.close();
    }

    if (logger != null) {
      logger.info("Closed Kafka exporter");
    }
  }

  @Override
  public void export(final Record record) {
    if (!recordHandler.isAllowed(record)) {
      logger.trace("Ignoring record {}", record);
      return;
    }

    final var producerRecord = recordHandler.transform(record);
    recordBatch.add(producerRecord);
    logger.trace("Added {} to the batch", producerRecord);
  }

  private void scheduleFlushBatchTask() {
    logger.trace("Rescheduling flush task in {}", config.getFlushInterval());
    flushTask = controller.scheduleCancellableTask(config.getFlushInterval(), this::flushBatchTask);
  }

  private void flushBatchTask() {
    try {
      recordBatch.flush();
    } finally {
      scheduleFlushBatchTask();
    }
  }

  private void updatePosition(final long position) {
    controller.updateLastExportedRecordPosition(position);
    logger.trace("Flushed batch and updated last exported record position to {}", position);
  }
}
