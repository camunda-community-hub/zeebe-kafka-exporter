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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import io.zeebe.exporter.kafka.config.raw.RawConfig;
import io.zeebe.exporter.record.Record;
import io.zeebe.protocol.clientapi.RecordType;
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

  private final RawConfig rawConfig = spy(new RawConfig());
  private final KafkaExporter exporter = new KafkaExporter();
  private final KafkaExporterConfig configuration = spy(new KafkaExporterConfig());
  private final ExporterTestHarness testHarness = new ExporterTestHarness(exporter);

  private MockProducer<Record, Record> mockProducer = new MockProducer<>(true, null, null);

  @Before
  public void setup() {
    configuration.records.defaults.topic = "zeebe";
    configuration.records.defaults.allowedTypes = EnumSet.allOf(RecordType.class);

    doAnswer(i -> configuration).when(rawConfig).parse();
    doAnswer(i -> mockProducer).when(configuration).newProducer();
  }

  @Test
  public void shouldExportRecords() {
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
    final ProducerRecord<Record, Record> expected =
        new ProducerRecord<>(configuration.records.defaults.topic, record, record);
    assertThat(mockProducer.history()).hasSize(1).containsExactly(expected);
  }

  @Test
  public void shouldUpdatePositionBasedOnCompletedRequests() {
    // given
    final int recordsCount = 4;

    // control how many are completed
    mockProducer = new MockProducer<>(false, null, null);

    configuration.async.maxInFlightRecords = recordsCount; // prevent blocking awaiting completion
    testHarness.configure(EXPORTER_ID, rawConfig);
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
  public void shouldAwaitCompletionOfEarliestRecordIfMaxRecordsInFlightReached() {
    // given
    final int recordsCount = 2;

    // since maxInFlightRecords is less than recordsCount, it will force awaiting
    // the completion of the next request and will update the position accordingly.
    // there's no blocking here because the MockProducer is configured to autocomplete.
    configuration.async.maxInFlightRecords = recordsCount - 1;
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final List<Record> records = testHarness.stream().export(recordsCount);

    // then
    assertThat(testHarness.getLastUpdatedPosition()).isEqualTo(records.get(0).getPosition());
  }

  @Test
  public void shouldFlushInFlightRecordsAndUpdatePositionOnClose() {
    // given
    final int recordsCount = 4;
    configuration.async.maxInFlightRecords = recordsCount;
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
  public void shouldDoNothingIfAlreadyClosed() {
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
  public void shouldThrowExceptionIfTimedOutWaitingForRecordCompletion() {
    // given
    mockProducer = new MockProducer<>(false, null, null);
    configuration.async.awaitInFlightRecordTimeout = Duration.ofMillis(1);
    configuration.async.maxInFlightRecords = 1;
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    testHarness.export();

    // then
    assertThatThrownBy(testHarness::export).isInstanceOf(KafkaExporterException.class);
  }

  @Test
  public void shouldUpdatePositionToLatestCompletedEventEvenIfOneRecordFails() {
    // given
    mockProducer = new MockProducer<>(false, null, null);
    configuration.async.maxInFlightRecords = 2;
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    final Record successful = testHarness.export();
    mockProducer.completeNext();
    testHarness.export();
    mockProducer.errorNext(new RuntimeException("failed"));

    // then
    assertThatThrownBy(this::checkInFlightRequests).isInstanceOf(KafkaExporterException.class);
    assertThat(testHarness.getLastUpdatedPosition()).isEqualTo(successful.getPosition());
  }

  private void completeNextRequests(int requestCount) {
    IntStream.rangeClosed(0, requestCount).forEach(i -> mockProducer.completeNext());
  }

  private void checkInFlightRequests() {
    testHarness.runScheduledTasks(KafkaExporter.IN_FLIGHT_RECORD_CHECKER_INTERVAL);
  }
}
