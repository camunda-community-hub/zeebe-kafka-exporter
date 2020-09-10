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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.config.raw.RawProducerConfig;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.util.JsonAssertions;
import io.zeebe.protocol.record.Record;
import io.zeebe.test.exporter.ExporterIntegrationRule;
import io.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;

public class KafkaExporterIT {
  private static final String TOPIC = "zeebe";

  @Rule public RecordingExporterTestWatcher testWatcher = new RecordingExporterTestWatcher();
  @Rule public KafkaContainer kafkaContainer = new KafkaContainer().withEmbeddedZookeeper();

  private ObjectMapper mapper = new ObjectMapper();
  private RawConfig exporterConfiguration;
  private ExporterIntegrationRule exporterIntegrationRule;

  @After
  public void tearDown() {
    if (exporterIntegrationRule != null) {
      exporterIntegrationRule.stop();
      exporterIntegrationRule = null;
    }

    exporterConfiguration = null;
  }

  @Test
  public void shouldExportRecords() {
    // given
    startZeebeBroker();

    // when
    exporterIntegrationRule.performSampleWorkload();

    // then
    exporterIntegrationRule.stop();
    assertRecordsExported();
    exporterIntegrationRule = null;
  }

  private void assertRecordsExported() {
    final Map<RecordId, String> records = consumeAllExportedRecords();
    exporterIntegrationRule.visitExportedRecords(r -> assertRecordExported(records, r));
  }

  private void assertRecordExported(
      final Map<RecordId, String> producedRecords, final Record<?> record) {
    final String producedRecord =
        producedRecords.get(new RecordId(record.getPartitionId(), record.getPosition()));

    assertThat(producedRecord).isNotNull();
    JsonAssertions.assertThat(producedRecord).isJsonEqualTo(record.toJson());
  }

  private Map<RecordId, String> consumeAllExportedRecords() {
    final Map<RecordId, String> records = new HashMap<>();
    final Duration timeout = Duration.ofSeconds(5);

    try (Consumer<Long, String> consumer = newConsumer()) {
      consumer
          .poll(timeout)
          .forEach(
              r -> {
                try {
                  final Map<String, Object> record = (Map) mapper.readValue(r.value(), Map.class);
                  final Number position = (Number) record.get("position");
                  final int partitionId = (Integer) record.get("partitionId");
                  records.put(new RecordId(partitionId, position.longValue()), r.value());
                } catch (JsonProcessingException e) {
                  throw new RuntimeException(e);
                }
              });
    }

    return records;
  }

  private Consumer<Long, String> newConsumer() {
    final Properties properties = consumerConfig();
    final Deserializer<Long> keyDeserializer = new LongDeserializer();
    final Deserializer<String> valueDeserializer = new StringDeserializer();

    keyDeserializer.configure(Maps.fromProperties(properties), true);
    valueDeserializer.configure(Maps.fromProperties(properties), false);
    final Consumer<Long, String> consumer =
        new KafkaConsumer<>(properties, keyDeserializer, valueDeserializer);
    consumer.subscribe(Collections.singletonList(TOPIC));

    return consumer;
  }

  private RawConfig newConfiguration() {
    final RawConfig configuration = new RawConfig();
    configuration.maxInFlightRecords = 30;
    configuration.producer = new RawProducerConfig();
    configuration.producer.servers = getKafkaServer();

    return configuration;
  }

  private void startZeebeBroker() {
    exporterConfiguration = newConfiguration();
    exporterIntegrationRule = new ExporterIntegrationRule();
    exporterIntegrationRule.configure("kafka", KafkaExporter.class, exporterConfiguration);
    exporterIntegrationRule.start();
  }

  private Properties consumerConfig() {
    final Properties properties = new Properties();
    properties.put("auto.commit.interval.ms", "100");
    properties.put("auto.offset.reset", "earliest");
    properties.put("bootstrap.servers", getKafkaServer());
    properties.put("enable.auto.commit", "true");
    properties.put("fetch.max.wait.ms", "200");
    properties.put("group.id", this.getClass().getName());
    properties.put("heartbeat.interval.ms", "100");
    properties.put("max.poll.records", String.valueOf(Integer.MAX_VALUE));
    properties.put("metadata.max.age.ms", "100");

    return properties;
  }

  private String getKafkaServer() {
    return String.format(
        "%s:%d",
        kafkaContainer.getContainerIpAddress(),
        kafkaContainer.getMappedPort(KafkaContainer.KAFKA_PORT));
  }
}
