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

import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.config.parser.MockConfigParser;
import io.zeebe.exporters.kafka.config.parser.RawConfigParser;
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.producer.MockKafkaProducerFactory;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.serde.RecordIdSerializer;
import io.zeebe.exporters.kafka.serde.RecordSerializer;
import io.zeebe.protocol.record.Record;
import io.zeebe.test.exporter.ExporterTestHarness;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("rawtypes")
public class KafkaExporterTest {
  private static final String EXPORTER_ID = "kafka";

  private final RawConfig rawConfig = new RawConfig();
  private final MockKafkaProducerFactory mockProducerFactory = new MockKafkaProducerFactory();
  private final MockConfigParser<RawConfig, Config> mockConfigParser =
      new MockConfigParser<>(new RawConfigParser());
  private final KafkaExporter exporter = new KafkaExporter(mockProducerFactory, mockConfigParser);
  private final ExporterTestHarness testHarness = new ExporterTestHarness(exporter);

  @Before
  public void setup() {
    mockProducerFactory.mockProducer =
        new MockProducer<>(true, new RecordIdSerializer(), new RecordSerializer());
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
    final ProducerRecord<RecordId, Record> expected =
        new ProducerRecord<>(
            mockConfigParser.config.getRecords().getDefaults().getTopic(),
            new RecordId(record.getPartitionId(), record.getPosition()),
            record);
    assertThat(mockProducerFactory.mockProducer.history()).hasSize(1).containsExactly(expected);
  }

  @Test
  public void shouldUpdatePositionBasedOnCompletedRequests() throws Exception {
    // given
    final int recordsCount = 4;

    // control how many are completed
    mockProducerFactory.mockProducer =
        new MockProducer<>(false, new RecordIdSerializer(), new RecordSerializer());
    rawConfig.maxInFlightRecords = recordsCount; // prevent blocking awaiting completion
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
    mockProducerFactory.mockProducer =
        new MockProducer<>(false, new RecordIdSerializer(), new RecordSerializer());
    final int recordsCount = 2;

    // since maxInFlightRecords is less than recordsCount, it will force awaiting
    // the completion of the next request and will update the position accordingly.
    // there's no blocking here because the MockProducer is configured to autocomplete.
    rawConfig.maxInFlightRecords = recordsCount - 1;
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
    rawConfig.maxInFlightRecords = recordsCount;
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
  public void shouldUpdatePositionToLatestCompletedEventEvenIfOneRecordFails() throws Exception {
    // given
    mockProducerFactory.mockProducer =
        new MockProducer<>(false, new RecordIdSerializer(), new RecordSerializer());
    rawConfig.maxInFlightRecords = 2;
    mockConfigParser.forceParse(rawConfig);

    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final Record successful = testHarness.export();
    mockProducerFactory.mockProducer.completeNext();
    final Record failed = testHarness.export();
    mockProducerFactory.mockProducer.errorNext(new RuntimeException("failed"));
    checkInFlightRequests();

    // then
    assertThat(testHarness.getLastUpdatedPosition())
        .isEqualTo(successful.getPosition())
        .isNotEqualTo(failed.getPosition());
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void shouldCloseExporterIfRecordFails() throws Exception {
    // given
    mockProducerFactory.mockProducer =
        new MockProducer<>(false, new RecordIdSerializer(), new RecordSerializer());
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    testHarness.export();
    mockProducerFactory.mockProducer.errorNext(new RuntimeException("failed"));
    checkInFlightRequests();
    testHarness.stream().export(5);

    // then
    assertThat(mockProducerFactory.mockProducer.history())
        .describedAs("should not have exported more records")
        .hasSize(1);
  }

  private void completeNextRequests(final int requestCount) {
    IntStream.rangeClosed(0, requestCount)
        .forEach(i -> mockProducerFactory.mockProducer.completeNext());
  }

  private void checkInFlightRequests() {
    testHarness.runScheduledTasks(KafkaExporter.IN_FLIGHT_RECORD_CHECKER_INTERVAL);
  }
}
