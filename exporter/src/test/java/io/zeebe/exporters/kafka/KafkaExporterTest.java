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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zeebe.exporter.api.record.Record;
import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.config.parser.MockParser;
import io.zeebe.exporters.kafka.config.parser.TomlConfigParser;
import io.zeebe.exporters.kafka.config.toml.TomlConfig;
import io.zeebe.exporters.kafka.producer.MockKafkaProducerFactory;
import io.zeebe.exporters.kafka.serde.RecordIdSerializer;
import io.zeebe.exporters.kafka.serde.generic.GenericRecordSerializer;
import io.zeebe.protocol.RecordType;
import io.zeebe.test.exporter.ExporterTestHarness;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;

public class KafkaExporterTest {
  private static final String EXPORTER_ID = "kafka";

  private final TomlConfig tomlConfig = new TomlConfig();
  private final MockKafkaProducerFactory mockProducerFactory = new MockKafkaProducerFactory();
  private final MockParser<TomlConfig, Config> mockConfigParser =
      new MockParser<>(new TomlConfigParser());
  private final KafkaExporter exporter = new KafkaExporter(mockProducerFactory, mockConfigParser);
  private final ExporterTestHarness testHarness = new ExporterTestHarness(exporter);
  private final Config configuration = mockConfigParser.parse(tomlConfig);

  @Before
  public void setup() {
    configuration.records.defaults.topic = "zeebe";
    configuration.records.defaults.allowedTypes = EnumSet.allOf(RecordType.class);
    mockProducerFactory.mockProducer =
        new MockProducer<>(true, new RecordIdSerializer(), new GenericRecordSerializer());
    mockConfigParser.config = configuration;
  }

  @Test
  public void shouldExportRecords() throws Exception {
    // given
    testHarness.configure(EXPORTER_ID, tomlConfig);
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
    final ProducerRecord<Record, Record> expected =
        new ProducerRecord<>(configuration.records.defaults.topic, record, record);
    assertThat(mockProducerFactory.mockProducer.history()).hasSize(1).containsExactly(expected);
  }

  @Test
  public void shouldUpdatePositionBasedOnCompletedRequests() throws Exception {
    // given
    final int recordsCount = 4;

    // control how many are completed
    mockProducerFactory.mockProducer = new MockProducer<>(false, null, null);

    configuration.maxInFlightRecords = recordsCount; // prevent blocking awaiting completion
    testHarness.configure(EXPORTER_ID, tomlConfig);
    testHarness.open();

    // when
    final List<Record> records = testHarness.stream().export(recordsCount);
    final int lastCompleted = records.size() - 1;
    completeNextRequests(lastCompleted);
    checkInFlightRequests();

    // then
    assertThat(testHarness.getLastUpdatedPosition())
        .isEqualTo(records.get(lastCompleted).getPosition());
  }

  @Test
  public void shouldAwaitCompletionOfEarliestRecordIfMaxRecordsInFlightReached() throws Exception {
    // given
    final int recordsCount = 2;

    // since maxInFlightRecords is less than recordsCount, it will force awaiting
    // the completion of the next request and will update the position accordingly.
    // there's no blocking here because the MockProducer is configured to autocomplete.
    configuration.maxInFlightRecords = recordsCount - 1;
    testHarness.configure(EXPORTER_ID, tomlConfig);
    testHarness.open();

    // when
    final List<Record> records = testHarness.stream().export(recordsCount);

    // then
    assertThat(testHarness.getLastUpdatedPosition()).isEqualTo(records.get(0).getPosition());
  }

  @Test
  public void shouldFlushInFlightRecordsAndUpdatePositionOnClose() throws Exception {
    // given
    final int recordsCount = 4;
    configuration.maxInFlightRecords = recordsCount;
    testHarness.configure(EXPORTER_ID, tomlConfig);
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
    testHarness.configure(EXPORTER_ID, tomlConfig);
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
  public void shouldThrowExceptionIfTimedOutWaitingForRecordCompletion() throws Exception {
    // given
    mockProducerFactory.mockProducer = new MockProducer<>(false, null, null);
    configuration.awaitInFlightRecordTimeout = Duration.ofMillis(1);
    configuration.maxInFlightRecords = 1;
    testHarness.configure(EXPORTER_ID, tomlConfig);
    testHarness.open();

    // when
    testHarness.export();

    // then
    assertThatThrownBy(testHarness::export).isInstanceOf(KafkaExporterException.class);
  }

  @Test
  public void shouldUpdatePositionToLatestCompletedEventEvenIfOneRecordFails() throws Exception {
    // given
    mockProducerFactory.mockProducer = new MockProducer<>(false, null, null);
    configuration.maxInFlightRecords = 2;
    testHarness.configure(EXPORTER_ID, tomlConfig);
    testHarness.open();

    // when
    final Record successful = testHarness.export();
    mockProducerFactory.mockProducer.completeNext();
    testHarness.export();
    mockProducerFactory.mockProducer.errorNext(new RuntimeException("failed"));

    // then
    assertThatThrownBy(this::checkInFlightRequests).isInstanceOf(KafkaExporterException.class);
    assertThat(testHarness.getLastUpdatedPosition()).isEqualTo(successful.getPosition());
  }

  private void completeNextRequests(int requestCount) {
    IntStream.rangeClosed(0, requestCount)
        .forEach(i -> mockProducerFactory.mockProducer.completeNext());
  }

  private void checkInFlightRequests() {
    testHarness.runScheduledTasks(KafkaExporter.IN_FLIGHT_RECORD_CHECKER_INTERVAL);
  }
}
