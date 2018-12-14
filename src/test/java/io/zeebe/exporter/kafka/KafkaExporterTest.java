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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.zeebe.exporter.kafka.configuration.ExporterConfiguration;
import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.record.RecordMetadata;
import io.zeebe.exporter.test.MockConfiguration;
import io.zeebe.exporter.test.MockContext;
import io.zeebe.exporter.test.MockController;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaExporterTest {

  private final KafkaExporter exporter = new KafkaExporter();
  private final ExporterConfiguration configuration = spy(new ExporterConfiguration());

  private final MockProducer<Record, Record> mockProducer = new MockProducer<>();
  private final MockContext mockContext = new MockContext();
  private final MockConfiguration<ExporterConfiguration> mockConfiguration =
      new MockConfiguration<>(configuration);
  private final MockController mockController = new MockController();
  private final Logger logger = LoggerFactory.getLogger(KafkaExporter.class);

  @Before
  public void setup() {
    configuration.setTopic("topic");
    mockConfiguration.setId("kafka");
    doAnswer(i -> mockProducer).when(configuration).newProducer("kafka");

    mockContext.setConfiguration(mockConfiguration);
    mockContext.setLogger(logger);
  }

  @Test
  public void shouldExportRecords() {
    // given
    final long position = 2L;
    final int partition = 1;
    final String json = "{\"foo\": \"bar\" }";
    final Record record = mockRecord(partition, position, json);

    // when
    exporter.configure(mockContext);
    exporter.open(mockController);
    exporter.export(record);

    // then
    assertThat(mockProducer.history())
        .hasSize(1)
        .containsExactly(new ProducerRecord<>(configuration.getTopic(), record, record));
  }

  @Test
  public void shouldUpdatePositionBasedOnCompletedRequests() {}

  private void checkInFlightRequests() {
    mockController.runScheduledTasks(KafkaExporter.IN_FLIGHT_RECORD_CHECKER_INTERVAL);
  }

  private Record mockRecord(int partitionId, long position, String json) {
    final RecordMetadata metadata = mock(RecordMetadata.class);
    when(metadata.getPartitionId()).thenReturn(partitionId);

    final Record record = mock(Record.class);
    when(record.getPosition()).thenReturn(position);
    when(record.getMetadata()).thenReturn(metadata);
    when(record.toJson()).thenReturn(json);

    return record;
  }
}
