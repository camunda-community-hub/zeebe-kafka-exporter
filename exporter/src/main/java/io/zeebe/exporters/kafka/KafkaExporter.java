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
import io.zeebe.exporter.api.context.ScheduledTask;
import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.config.parser.ConfigParser;
import io.zeebe.exporters.kafka.config.parser.RawConfigParser;
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.producer.DefaultKafkaProducerFactory;
import io.zeebe.exporters.kafka.producer.KafkaProducerFactory;
import io.zeebe.exporters.kafka.producer.RecordBatch;
import io.zeebe.exporters.kafka.producer.TransactionBoundedRecordBatch;
import io.zeebe.exporters.kafka.record.KafkaRecordFilter;
import io.zeebe.exporters.kafka.record.RecordHandler;
import io.zeebe.exporters.kafka.serde.RecordSerializer;
import io.zeebe.protocol.record.Record;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;

/** Implementation of a Zeebe exporter producing serialized records to a given Kafka topic. */
public final class KafkaExporter implements Exporter {
  private final KafkaProducerFactory producerFactory;
  private final ConfigParser<RawConfig, Config> configParser;
  private final String producerId;

  private Controller controller;
  private Logger logger;
  private Config config;
  private RecordHandler recordHandler;
  private ScheduledTask flushTask;
  private RecordBatch recordBatch;

  // the constructor is used by the Zeebe broker to instantiate it
  @SuppressWarnings("unused")
  public KafkaExporter() {
    this(new DefaultKafkaProducerFactory(), new RawConfigParser());
  }

  public KafkaExporter(
      final @NonNull KafkaProducerFactory producerFactory,
      final @NonNull ConfigParser<RawConfig, Config> configParser) {
    this(producerFactory, configParser, UUID.randomUUID().toString());
  }

  public KafkaExporter(
      final @NonNull KafkaProducerFactory producerFactory,
      final @NonNull ConfigParser<RawConfig, Config> configParser,
      final @NonNull String producerId) {
    this.producerFactory = Objects.requireNonNull(producerFactory);
    this.configParser = Objects.requireNonNull(configParser);
    this.producerId = producerId;
  }

  @Override
  public void configure(final Context context) {
    logger = context.getLogger();

    final var rawConfig = context.getConfiguration().instantiate(RawConfig.class);
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
        new TransactionBoundedRecordBatch(
            producerFactory,
            config.getProducer(),
            producerId,
            config.getMaxBatchSize(),
            this::updatePosition,
            logger);

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

    recordBatch.flush();
    recordBatch.close();

    logger.info("Closed Kafka exporter");
  }

  @Override
  public void export(final Record record) {
    if (!recordHandler.isAllowed(record)) {
      logger.trace("Ignoring record {}", record);
      return;
    }

    final var producerRecord = recordHandler.transform(record);
    recordBatch.add(producerRecord);
  }

  private void scheduleFlushBatchTask() {
    logger.trace("Rescheduling flush task in {}", config.getCommitInterval());
    flushTask =
        controller.scheduleCancellableTask(config.getCommitInterval(), this::flushBatchTask);
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
