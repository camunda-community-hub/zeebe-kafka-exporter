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
import static org.assertj.core.api.Assertions.assertThatCode;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.config.parser.MockConfigParser;
import io.zeebe.exporters.kafka.config.parser.RawConfigParser;
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.producer.MockKafkaProducerFactory;
import io.zeebe.exporters.kafka.record.RecordHandler;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.serde.RecordIdSerializer;
import io.zeebe.protocol.record.Record;
import io.zeebe.test.exporter.ExporterTestHarness;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.kafka.clients.consumer.internals.NoAvailableBrokersException;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.CorruptRecordException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.NotEnoughReplicasAfterAppendException;
import org.apache.kafka.common.errors.NotEnoughReplicasException;
import org.apache.kafka.common.errors.OffsetMetadataTooLarge;
import org.apache.kafka.common.errors.OffsetOutOfRangeException;
import org.apache.kafka.common.errors.RecordBatchTooLargeException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownServerException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({"rawtypes", "ResultOfMethodCallIgnored"})
public class KafkaExporterTest {
  private static final String EXPORTER_ID = "kafka";

  private final RawConfig rawConfig = new RawConfig();
  private final MockKafkaProducerFactory mockProducerFactory =
      new MockKafkaProducerFactory(this::newMockProducer);
  private final MockConfigParser<RawConfig, Config> mockConfigParser =
      new MockConfigParser<>(new RawConfigParser());
  private final KafkaExporter exporter = new KafkaExporter(mockProducerFactory, mockConfigParser);
  private final ExporterTestHarness testHarness = new ExporterTestHarness(exporter);

  @Before
  public void setup() {
    mockConfigParser.config = mockConfigParser.parse(rawConfig);
  }

  @Test
  public void shouldExportRecords() throws Exception {
    // given
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final Record record =
        testHarness.export(
            r -> {
              r.setPosition(2L);
              r.getMetadata().setPartitionId(1);
              r.getValue().setJson("{\"foo\": \"bar\" }");
            });

    // then
    final ProducerRecord<RecordId, byte[]> expected =
        new RecordHandler(mockConfigParser.config.getRecords()).transform(record);
    assertThat(mockProducerFactory.mockProducer.history()).hasSize(1);

    final ProducerRecord<RecordId, byte[]> producedRecord =
        mockProducerFactory.mockProducer.history().get(0);
    assertThat(producedRecord.topic()).isEqualTo(expected.topic());
    assertThat(producedRecord.key()).isEqualTo(expected.key());
    assertThat(producedRecord.value()).isEqualTo(expected.value());
  }

  @Test
  public void shouldUpdatePositionBasedOnCompletedRequests() throws Exception {
    // given
    final int recordsCount = 4;

    // control how many are completed
    mockProducerFactory.mockProducer = new MockProducer<>(false, null, null);
    rawConfig.maxBatchSize = Integer.MAX_VALUE; // prevent blocking awaiting completion
    mockConfigParser.forceParse(rawConfig);

    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final List<Record> records = testHarness.stream().export(recordsCount);
    final int lastCompleted = records.size() - 2;
    completeNextRequests(lastCompleted);
    checkInFlightRequests();

    // then
    assertThat(testHarness.getLastUpdatedPosition())
        .isEqualTo(records.get(lastCompleted).getPosition());
  }

  @Test
  public void shouldBlockIfRequestQueueFull() throws Exception {
    // given
    mockProducerFactory.mockProducer = new MockProducer<>(false, null, null);

    // since maxBatchSize is pretty small, it should accept the first record but immediately block
    // on the second one (as the batch is already full). the completion of the next request and will
    // update the position accordingly. there's no blocking here because the MockProducer is
    // configured to autocomplete.
    rawConfig.maxBatchSize = 1;
    mockConfigParser.forceParse(rawConfig);

    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final Record exported = testHarness.export();
    mockProducerFactory.mockProducer.completeNext();
    final Record notExported = testHarness.export();
    checkInFlightRequests();

    // then
    assertThat(testHarness.getLastUpdatedPosition())
        .isEqualTo(exported.getPosition())
        .isNotEqualTo(notExported.getPosition());
  }

  @Test
  public void shouldUpdatePositionOnClose() throws Exception {
    // given
    final int recordsCount = 4;
    rawConfig.maxBatchSize = recordsCount;
    mockConfigParser.forceParse(rawConfig);

    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final List<Record> records = testHarness.stream().export(recordsCount);

    // then
    testHarness.close();
    assertThat(testHarness.getLastUpdatedPosition())
        .isEqualTo(records.get(recordsCount - 1).getPosition());
  }

  @Test
  public void shouldDoNothingIfAlreadyClosed() throws Exception {
    // given
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();
    testHarness.close();

    // when
    testHarness.export();
    checkInFlightRequests();

    // then
    assertThat(testHarness.getLastUpdatedPosition()).isLessThan(testHarness.getPosition());
    assertThatCode(testHarness::export).doesNotThrowAnyException();
    assertThatCode(testHarness::close).doesNotThrowAnyException();
  }

  @Test
  public void shouldRetryRecordOnException() throws Exception {
    // given
    mockProducerFactory.mockProducer = new MockProducer<>(false, null, null);
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    testHarness.export();
    mockProducerFactory.mockProducer.errorNext(new RuntimeException("failed"));
    checkInFlightRequests();
    testHarness.stream().export(2);

    // then
    assertThat(mockProducerFactory.mockProducer.history())
        .describedAs("should have the produced the exact amount of exported records")
        .hasSize(3);
  }

  @Test
  public void shouldRetryRecoverableErrors() throws Exception {
    // given
    // this is an non exhaustive list of recoverable exceptions - see all children of
    // org.apache.kafka.common.errors.RetriableExceptions applicable to a producer
    final List<RuntimeException> exceptions =
        List.of(
            new CorruptRecordException(),
            new NoAvailableBrokersException(),
            new NotEnoughReplicasAfterAppendException("error"),
            new NotEnoughReplicasException(),
            new OffsetOutOfRangeException("error"),
            new TimeoutException(),
            new UnknownTopicOrPartitionException());
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final List<Record> records = testHarness.stream().export(6);
    exceptions.forEach(t -> mockProducerFactory.mockProducer.errorNext(t));
    checkInFlightRequests();

    // then
    assertThat(mockProducerFactory.mockProducer.history())
        .describedAs("should have the produced the exact amount of exported records")
        .hasSize(6);
    assertThat(testHarness.getLastUpdatedPosition())
        .as("should have acknowledged the last record position")
        .isEqualTo(records.get(records.size() - 1).getPosition());
  }

  @Test
  public void shouldRetryUnrecoverableErrors() throws Exception {
    // given
    final List<RuntimeException> exceptions =
        List.of(
            new InvalidTopicException(),
            new OffsetMetadataTooLarge(),
            new RecordBatchTooLargeException(),
            new RecordTooLargeException(),
            new UnknownServerException());
    // disable auto completion for better control of the retry mechanism
    mockProducerFactory.mockProducerSupplier =
        () -> new MockProducer<>(false, new RecordIdSerializer(), new ByteArraySerializer());
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final List<Record> records = testHarness.stream().export(exceptions.size() + 2);
    for (int i = 0; i < exceptions.size(); i++) {
      final var previousMockProducer = mockProducerFactory.mockProducer;
      mockProducerFactory.mockProducer.errorNext(exceptions.get(i));
      checkInFlightRequests();
      mockProducerFactory.mockProducer.completeNext();
      checkInFlightRequests();

      assertThat(mockProducerFactory.mockProducer.history())
          .as("should have retried all records to the producer")
          .hasSize(records.size() - i);
      assertThat(testHarness.getLastUpdatedPosition())
          .as("should not update position on error")
          .isEqualTo(records.get(i).getPosition());
      assertThat(mockProducerFactory.mockProducer)
          .as("should close producer on unrecoverable error")
          .isNotSameAs(previousMockProducer);
    }

    // complete the rest of the calls
    mockProducerFactory
        .mockProducer
        .history()
        .forEach(ignored -> mockProducerFactory.mockProducer.completeNext());
    checkInFlightRequests();

    // then
    assertThat(testHarness.getLastUpdatedPosition())
        .as("should have acknowledged the last record position")
        .isEqualTo(records.get(records.size() - 1).getPosition());
  }

  @NonNull
  private MockProducer<RecordId, byte[]> newMockProducer() {
    return new MockProducer<>(true, new RecordIdSerializer(), new ByteArraySerializer());
  }

  private void completeNextRequests(final int requestCount) {
    IntStream.rangeClosed(0, requestCount)
        .forEach(i -> mockProducerFactory.mockProducer.completeNext());
  }

  private void checkInFlightRequests() {
    testHarness.runScheduledTasks(KafkaExporter.IN_FLIGHT_RECORD_CHECKER_INTERVAL);
  }
}
