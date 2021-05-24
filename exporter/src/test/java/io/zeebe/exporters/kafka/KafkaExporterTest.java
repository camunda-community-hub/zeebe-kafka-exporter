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

import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.test.exporter.ExporterTestHarness;
import io.camunda.zeebe.test.exporter.record.MockRecordMetadata;
import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.config.parser.MockConfigParser;
import io.zeebe.exporters.kafka.config.parser.RawConfigParser;
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordsConfig;
import io.zeebe.exporters.kafka.producer.RecordBatchStub;
import io.zeebe.exporters.kafka.record.RecordHandler;
import io.zeebe.exporters.kafka.serde.RecordId;
import java.util.stream.Collectors;
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
  private final RecordBatchStub.Factory batchStubFactory = new RecordBatchStub.Factory();
  private final KafkaExporter exporter = new KafkaExporter(batchStubFactory, mockConfigParser);
  private final ExporterTestHarness testHarness = new ExporterTestHarness(exporter);

  @Test
  void shouldAddRecordToBatchOnExport() throws Exception {
    // given
    rawConfig.maxBatchSize = 5;
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final var records = testHarness.stream().export(5);

    // then
    final var expectedIds =
        records.stream()
            .map(r -> new RecordId(r.getPartitionId(), r.getPosition()))
            .collect(Collectors.toList());
    assertThat(batchStubFactory.stub.getPendingRecords())
        .as("the records were added to the batch in order")
        .extracting(ProducerRecord::key)
        .containsExactlyElementsOf(expectedIds);
    assertThat(batchStubFactory.stub.getFlushedRecords())
        .as("no records were flushed yet")
        .isEmpty();
  }

  @Test
  void shouldUseCorrectSerializer() throws Exception {
    // given
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();
    final var recordHandler = new RecordHandler(mockConfigParser.config.getRecords());

    // when
    final var json = "{\"a\": 1}";
    final var record = testHarness.export(r -> r.setJson(json));

    // then
    final var expectedRecord = recordHandler.transform(record);
    assertThat(batchStubFactory.stub.getPendingRecords())
        .as("the serialized record was added to the batch")
        .extracting("topic", "key", "value")
        .containsExactly(
            tuple(expectedRecord.topic(), expectedRecord.key(), expectedRecord.value()));
  }

  @Test
  void shouldSkipDisallowedRecords() throws Exception {
    // given
    rawConfig.records = new RawRecordsConfig();
    rawConfig.records.deployment = new RawRecordConfig();
    rawConfig.records.deployment.type = "";
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    testHarness.export(
        r -> r.setMetadata(new MockRecordMetadata().setValueType(ValueType.DEPLOYMENT)));

    // then
    assertThat(batchStubFactory.stub.getPendingRecords())
        .as("disallowed record should not be added to the batch")
        .isEmpty();
  }

  @Test
  void shouldFlushOnScheduledTask() throws Exception {
    // given
    rawConfig.maxBatchSize = 5;
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final var records = testHarness.stream().export(5);
    triggerFlushTask();

    // then
    final var expectedIds =
        records.stream()
            .map(r -> new RecordId(r.getPartitionId(), r.getPosition()))
            .collect(Collectors.toList());
    assertThat(batchStubFactory.stub.getFlushedRecords())
        .as("the records were added to the batch in order")
        .extracting(ProducerRecord::key)
        .containsExactlyElementsOf(expectedIds);
    assertThat(batchStubFactory.stub.getPendingRecords())
        .as("no pending records after flush")
        .isEmpty();
  }

  @Test
  void shouldUpdatePositionOnFlush() throws Exception {
    // given
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final var records = testHarness.stream().export(5);
    triggerFlushTask();

    // then
    assertThat(testHarness.getLastUpdatedPosition())
        .as("position should be updated since after flush")
        .isEqualTo(records.get(4).getPosition());
  }

  @Test
  void shouldRescheduleFlushTaskEvenOnException() throws Exception {
    // given
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final var records = testHarness.stream().export(2);
    batchStubFactory.stub.flushException = new RuntimeException("failed to flush");
    assertThatThrownBy(this::triggerFlushTask).isEqualTo(batchStubFactory.stub.flushException);
    batchStubFactory.stub.flushException = null;
    triggerFlushTask();

    // then
    assertThat(testHarness.getLastUpdatedPosition())
        .as("position should be updated since we managed to flush after the second try")
        .isEqualTo(records.get(1).getPosition());
  }

  @Test
  void shouldFlushBatchOnClose() throws Exception {
    // given
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final var records = testHarness.stream().export(2);
    testHarness.close();

    // then
    assertThat(testHarness.getLastUpdatedPosition())
        .as("position should be updated since we managed to flush after the second try")
        .isEqualTo(records.get(1).getPosition());
    assertThat(batchStubFactory.stub.isClosed())
        .as("batch should be closed on exporter close")
        .isTrue();
  }

  @Test
  void shouldRescheduleFlush() throws Exception {
    // given
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    triggerFlushTask();
    final var records = testHarness.stream().export(2);
    triggerFlushTask();

    // then
    assertThat(testHarness.getLastUpdatedPosition())
        .as("position should be updated after triggering the second flush task")
        .isEqualTo(records.get(1).getPosition());
  }

  private void triggerFlushTask() {
    mockConfigParser.parse(rawConfig);
    testHarness.runScheduledTasks(mockConfigParser.config.getFlushInterval());
  }
}
