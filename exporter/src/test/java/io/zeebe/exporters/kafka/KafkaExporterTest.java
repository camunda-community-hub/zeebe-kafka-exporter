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
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("rawtypes")
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
    mockProducerFactory.mockProducer.commitTransaction();
    assertThat(mockProducerFactory.mockProducer.history()).hasSize(1);

    final ProducerRecord<RecordId, byte[]> producedRecord =
        mockProducerFactory.mockProducer.history().get(0);
    assertThat(producedRecord.topic()).isEqualTo(expected.topic());
    assertThat(producedRecord.key()).isEqualTo(expected.key());
    assertThat(producedRecord.value()).isEqualTo(expected.value());
  }

  @Test
  public void shouldBlockIfRequestQueueFull() throws Exception {
    // given
    mockProducerFactory.mockProducer =
        new MockProducer<>(false, new RecordIdSerializer(), new ByteArraySerializer());

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

    // then - no need to check the position is updated since when full it will have done so already
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
    mockProducerFactory.mockProducer =
        new MockProducer<>(false, new RecordIdSerializer(), new ByteArraySerializer());
    testHarness.configure(EXPORTER_ID, rawConfig);
    testHarness.open();

    // when
    testHarness.export();
    mockProducerFactory.mockProducer.fenceProducer();
    checkInFlightRequests();
    testHarness.stream().export(2);
    mockProducerFactory.mockProducer.commitTransaction();

    // then
    assertThat(mockProducerFactory.mockProducer.history())
        .describedAs("should have the produced the exact amount of exported records")
        .hasSize(3);
  }

  @NonNull
  private MockProducer<RecordId, byte[]> newMockProducer() {
    return new MockProducer<>(true, new RecordIdSerializer(), new ByteArraySerializer());
  }

  private void checkInFlightRequests() {
    testHarness.runScheduledTasks(mockConfigParser.config.getCommitInterval());
  }
}
