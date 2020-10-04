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

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.config.raw.RawProducerConfig;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.serde.RecordIdDeserializer;
import io.zeebe.exporters.kafka.util.JsonAssertions;
import io.zeebe.protocol.record.Record;
import io.zeebe.test.exporter.ExporterIntegrationRule;
import io.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KafkaExporterIT {
  private static final String TOPIC = "zeebe";
  private static final int PARTITION_COUNT = 3;

  @Rule public RecordingExporterTestWatcher testWatcher = new RecordingExporterTestWatcher();

  @Rule
  public KafkaContainer kafkaContainer =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag("5.5.1"))
          .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
          .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
          .withEmbeddedZookeeper();

  private RawConfig exporterConfiguration;
  private ExporterIntegrationRule exporterIntegrationRule;

  @Before
  public void setUp() {
    // provision Kafka topics - this is difficult at the moment to achieve purely via
    // configuration, so we do it as a pre-step
    final NewTopic topic = new NewTopic(TOPIC, PARTITION_COUNT, (short) 1);
    try (final AdminClient admin =
        AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaServer()))) {
      admin.createTopics(List.of(topic));
    }
  }

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
    final Map<RecordId, ConsumerRecord<RecordId, String>> records = consumeAllExportedRecords();
    exporterIntegrationRule.visitExportedRecords(r -> assertRecordExported(records, r));

    // in order to not depend on implementation detail of how the partitioner works, group the
    // records as they were produced in partition, and assert that the order is maintained amongst
    // them, and all records of the same Zeebe partition were produced on the same Kafka partition
    final Map<Integer, List<ConsumerRecord<RecordId, String>>> perZeebePartition =
        collectConsumedRecordsByZeebePartition(records);

    // ensure all have the same Kafka partition, and that all are in order by position - as
    // they are already sorted by offset (so Kafka order), we can ensure they are also
    // maintaining the same Zeebe order (via position)
    perZeebePartition.forEach(
        (partitionId, list) -> {
          final int kafkaPartition = list.get(0).partition();
          assertThat(list)
              .allSatisfy(r -> assertThat(r.partition()).isEqualTo(kafkaPartition))
              .isSortedAccordingTo(
                  Comparator.comparing(r -> r.key().getPosition(), Long::compareTo));
        });
  }

  private void assertRecordExported(
      final Map<RecordId, ConsumerRecord<RecordId, String>> producedRecords,
      final Record<?> record) {
    final RecordId recordId = new RecordId(record.getPartitionId(), record.getPosition());
    final ConsumerRecord<RecordId, String> producedRecord = producedRecords.get(recordId);

    assertThat(producedRecord).isNotNull();
    JsonAssertions.assertThat(producedRecord.value()).isJsonEqualTo(record.toJson());
  }

  @NonNull
  private Map<RecordId, ConsumerRecord<RecordId, String>> consumeAllExportedRecords() {
    final Map<RecordId, ConsumerRecord<RecordId, String>> records = new LinkedHashMap<>();
    final Duration timeout = Duration.ofSeconds(5);

    try (Consumer<RecordId, String> consumer = newConsumer()) {
      consumer.poll(timeout).forEach(r -> records.put(r.key(), r));
    }

    return records;
  }

  @NonNull
  private Consumer<RecordId, String> newConsumer() {
    final Properties properties = consumerConfig();
    final Deserializer<RecordId> keyDeserializer = new RecordIdDeserializer();
    final Deserializer<String> valueDeserializer = new StringDeserializer();

    keyDeserializer.configure(Maps.fromProperties(properties), true);
    valueDeserializer.configure(Maps.fromProperties(properties), false);
    final Consumer<RecordId, String> consumer =
        new KafkaConsumer<>(properties, keyDeserializer, valueDeserializer);
    consumer.subscribe(Collections.singletonList(TOPIC));

    return consumer;
  }

  @NonNull
  private RawConfig newConfiguration() {
    final RawConfig configuration = new RawConfig();
    configuration.maxBatchSize = 32 * 1024 * 1024;
    configuration.producer = new RawProducerConfig();
    configuration.producer.servers = getKafkaServer();

    return configuration;
  }

  private void startZeebeBroker() {
    exporterConfiguration = newConfiguration();
    exporterIntegrationRule = new ExporterIntegrationRule();
    exporterIntegrationRule.configure("kafka", KafkaExporter.class, exporterConfiguration);
    exporterIntegrationRule.getBrokerConfig().getCluster().setPartitionsCount(PARTITION_COUNT);
    exporterIntegrationRule.start();
  }

  @NonNull
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

  @NonNull
  private String getKafkaServer() {
    return String.format(
        "%s:%d",
        kafkaContainer.getContainerIpAddress(),
        kafkaContainer.getMappedPort(KafkaContainer.KAFKA_PORT));
  }

  @NonNull
  private Map<Integer, List<ConsumerRecord<RecordId, String>>>
      collectConsumedRecordsByZeebePartition(
          final Map<RecordId, ConsumerRecord<RecordId, String>> records) {
    final Map<Integer, List<ConsumerRecord<RecordId, String>>> perZeebePartition = new HashMap<>();
    records
        .values()
        .forEach(
            r -> {
              final List<ConsumerRecord<RecordId, String>> perZeebePartitionList =
                  perZeebePartition.computeIfAbsent(
                      r.key().getPartitionId(), partitionId -> new ArrayList<>());

              perZeebePartitionList.add(r);
              perZeebePartitionList.sort(
                  Comparator.comparing(ConsumerRecord::offset, Long::compareTo));
            });
    return perZeebePartition;
  }
}
