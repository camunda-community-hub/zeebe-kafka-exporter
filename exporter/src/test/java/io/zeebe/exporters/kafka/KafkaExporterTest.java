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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.camunda.zeebe.exporter.test.ExporterTestConfiguration;
import io.camunda.zeebe.exporter.test.ExporterTestContext;
import io.camunda.zeebe.exporter.test.ExporterTestController;
import io.camunda.zeebe.protocol.record.ImmutableRecord;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.DeploymentIntent;
import io.camunda.zeebe.protocol.record.value.ImmutableDeploymentRecordValue;
import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.config.parser.MockConfigParser;
import io.zeebe.exporters.kafka.config.parser.RawConfigParser;
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordsConfig;
import io.zeebe.exporters.kafka.producer.RecordBatchStub;
import io.zeebe.exporters.kafka.record.RecordHandler;
import io.zeebe.exporters.kafka.serde.RecordId;
import java.time.Duration;
import java.util.List;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@SuppressWarnings("rawtypes")
@Execution(ExecutionMode.CONCURRENT)
final class KafkaExporterTest {
  private static final String EXPORTER_ID = "kafka";

  private final RawConfig rawConfig = new RawConfig();
  private final MockConfigParser<RawConfig, Config> mockConfigParser =
    new MockConfigParser<>(new RawConfigParser());

  private final ExporterTestContext context =
    new ExporterTestContext().setConfiguration(new ExporterTestConfiguration<>("test", rawConfig));
  private final ExporterTestController controller = new ExporterTestController();

  private final RecordBatchStub.Factory batchStubFactory = new RecordBatchStub.Factory();
  private final KafkaExporter exporter = new KafkaExporter(batchStubFactory, mockConfigParser);

  @Test
  void shouldAddRecordToBatchOnExport() {
    // given
    rawConfig.maxBatchSize = 5;
    exporter.configure(context);
    exporter.open(controller);

    final Record<?> record = recordFixture();

    // when
    exporter.export(record);

    // then
    final var expectedIds = new RecordId(record.getPartitionId(), record.getPosition());
    assertThat(batchStubFactory.stub.getPendingRecords())
        .as("the records were added to the batch in order")
        .extracting(ProducerRecord::key)
        .containsExactlyElementsOf(List.of(expectedIds));
    assertThat(batchStubFactory.stub.getFlushedRecords())
        .as("no records were flushed yet")
        .isEmpty();
  }

  @Test
  void shouldUseCorrectSerializer() {
    // given
    exporter.configure(context);
    exporter.open(controller);
    final var recordHandler = new RecordHandler(mockConfigParser.config.getRecords());

    final Record<?> record = recordFixture();

    // when
    exporter.export(record);

    // then
    final var expectedRecord = recordHandler.transform(record);
    assertThat(batchStubFactory.stub.getPendingRecords())
        .as("the serialized record was added to the batch")
        .extracting("topic", "key", "value")
        .containsExactly(
            tuple(expectedRecord.topic(), expectedRecord.key(), expectedRecord.value()));
  }


  @Test
  void shouldSkipDisallowedRecords() {
    // given
    rawConfig.records = new RawRecordsConfig();
    rawConfig.records.deployment = new RawRecordConfig();
    rawConfig.records.deployment.type = "";
    exporter.configure(context);
    exporter.open(controller);

    final Record<?> record = recordFixture();

    // when
    exporter.export(record);

    // then
    assertThat(batchStubFactory.stub.getPendingRecords())
        .as("disallowed record should not be added to the batch")
        .isEmpty();
  }


  @Test
  void shouldFlushOnScheduledTask() {
    // given
    exporter.configure(context);
    exporter.open(controller);

    final Record<?> record = recordFixture();

    // when
    exporter.export(record);
    controller.runScheduledTasks(Duration.ofSeconds(10));

    // then
    final var expectedIds = new RecordId(record.getPartitionId(), record.getPosition());
    assertThat(batchStubFactory.stub.getFlushedRecords())
        .as("the records were added to the batch in order")
        .extracting(ProducerRecord::key)
        .containsExactlyElementsOf(List.of(expectedIds));
    assertThat(batchStubFactory.stub.getPendingRecords())
        .as("no pending records after flush")
        .isEmpty();
  }

  @Test
  void shouldUpdatePositionOnFlush() {
    // given
    exporter.configure(context);
    exporter.open(controller);

    final Record<?> record = recordFixture();

    // when
    exporter.export(record);
    controller.runScheduledTasks(Duration.ofSeconds(10));

    // then
    assertThat(controller.getPosition())
        .as("position should be updated since after flush")
        .isEqualTo(record.getPosition());
  }

  @Test
  void shouldRescheduleFlushTaskEvenOnException() {
    // given
    exporter.configure(context);
    exporter.open(controller);

    final Record<?> record = recordFixture();

    // when
    exporter.export(record);
    batchStubFactory.stub.flushException = new RuntimeException("failed to flush");
    assertThatThrownBy(() -> controller.runScheduledTasks(Duration.ofSeconds(10))).isEqualTo(batchStubFactory.stub.flushException);
    batchStubFactory.stub.flushException = null;
    controller.runScheduledTasks(Duration.ofSeconds(10));

    // then
    assertThat(controller.getPosition())
        .as("position should be updated since we managed to flush after the second try")
        .isEqualTo(record.getPosition());
  }

  @Test
  void shouldFlushBatchOnClose() {
    // given
    exporter.configure(context);
    exporter.open(controller);
    final Record<?> record = recordFixture();

    // when
    exporter.export(record);
    exporter.close();

    // then
    assertThat(controller.getPosition())
        .as("position should be updated since we managed to flush after the second try")
        .isEqualTo(record.getPosition());
    assertThat(batchStubFactory.stub.isClosed())
        .as("batch should be closed on exporter close")
        .isTrue();
  }

  @Test
  void shouldRescheduleFlush() {
    // given
    exporter.configure(context);
    exporter.open(controller);

    // when
    controller.runScheduledTasks(Duration.ofSeconds(10));
    exporter.export(recordFixture());
    controller.runScheduledTasks(Duration.ofSeconds(10));

    // then
    assertThat(controller.getPosition())
        .as("position should be updated after triggering the second flush task")
        .isEqualTo(0);
  }

  Record<?> recordFixture() {
    return
      ImmutableRecord.builder()
        .withIntent(DeploymentIntent.CREATED)
        .withRecordType(RecordType.EVENT)
        .withValueType(ValueType.DEPLOYMENT)
        .withValue(ImmutableDeploymentRecordValue
          .builder()
          .build())
        .build();
  }
}
