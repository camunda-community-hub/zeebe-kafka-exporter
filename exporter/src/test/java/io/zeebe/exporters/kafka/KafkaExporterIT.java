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

import static com.github.charithe.kafka.KafkaJunitExtensionConfig.ALLOCATE_RANDOM_PORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.github.charithe.kafka.EphemeralKafkaBroker;
import com.github.charithe.kafka.KafkaHelper;
import com.google.protobuf.Message;
import io.zeebe.exporter.proto.RecordTransformer;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.exporter.record.Record;
import io.zeebe.exporters.kafka.config.toml.TomlConfig;
import io.zeebe.exporters.kafka.serde.RecordIdDeserializer;
import io.zeebe.exporters.kafka.serde.generic.GenericRecord;
import io.zeebe.exporters.kafka.serde.generic.GenericRecordDeserializer;
import io.zeebe.test.exporter.ExporterIntegrationRule;
import io.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.curator.test.InstanceSpec;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class KafkaExporterIT {
  private static final String KEYSTORE_LOCATION =
      System.getProperty("io.zeebe.exporters.kafka.keyStore", "src/test/resources/keystore.jks");
  private static final String TOPIC = "zeebe";

  @Rule public RecordingExporterTestWatcher testWatcher = new RecordingExporterTestWatcher();

  private TomlConfig exporterConfiguration;
  private ExporterIntegrationRule exporterIntegrationRule;
  private EphemeralKafkaBroker kafkaBroker;
  private KafkaHelper kafkaHelper;

  @After
  public void tearDown() throws ExecutionException, InterruptedException {
    if (exporterIntegrationRule != null) {
      exporterIntegrationRule.stop();
      exporterIntegrationRule = null;
    }

    if (kafkaBroker != null) {
      kafkaBroker.stop();
      kafkaBroker = null;
    }

    kafkaHelper = null;
    exporterConfiguration = null;
  }

  @Parameterized.Parameter(0)
  public String name;

  @Parameterized.Parameter(1)
  public Consumer<TomlConfig> exporterConfigurator;

  @Parameterized.Parameter(2)
  public BiConsumer<Properties, Integer> kafkaConfigurator;

  @Parameterized.Parameters(name = "{0}")
  public static Object[][] data() {
    return new Object[][] {
      new Object[] {"defaults", exporter(c -> {}), kafka((p, port) -> {})},
      new Object[] {
        "ssl",
        exporter(c -> c.producer.config = newSslConfiguration()),
        kafka((p, port) -> p.putAll(newBrokerSslConfiguration(port)))
      }
    };
  }

  @Test
  public void shouldExportRecords() throws Exception {
    // given
    startKafkaBroker();
    startZeebeBroker();

    // when
    exporterIntegrationRule.performSampleWorkload();

    // then
    assertRecordsExported();
  }

  private void assertRecordsExported() {
    final Map<Schema.RecordId, Message> records = consumeAllExportedRecords();
    exporterIntegrationRule.visitExportedRecords(r -> assertRecordExported(records, r));
  }

  private void assertRecordExported(
      Map<Schema.RecordId, Message> producedRecords, Record<?> record) {
    assertThat(producedRecords)
        .contains(
            entry(
                RecordTransformer.toRecordId(record), RecordTransformer.toProtobufMessage(record)));
  }

  private Map<Schema.RecordId, Message> consumeAllExportedRecords() {
    final Map<Schema.RecordId, Message> records = new HashMap<>();
    final Duration timeout = Duration.ofSeconds(5);

    try (KafkaConsumer<Schema.RecordId, GenericRecord> consumer = newConsumer()) {
      consumer.poll(timeout).forEach(r -> records.put(r.key(), r.value().getMessage()));
    }

    return records;
  }

  private KafkaConsumer<Schema.RecordId, GenericRecord> newConsumer() {
    final Properties properties = new Properties();
    properties.put("group.id", this.getClass().getName());
    properties.put("enable.auto.commit", "true");
    properties.put("auto.commit.interval.ms", "100");
    properties.put("max.poll.records", String.valueOf(Integer.MAX_VALUE));

    if (exporterConfiguration.producer.config != null) {
      properties.putAll(exporterConfiguration.producer.config);
    }

    final KafkaConsumer<Schema.RecordId, GenericRecord> consumer =
        kafkaHelper.createConsumer(
            new RecordIdDeserializer(), new GenericRecordDeserializer(), properties);
    consumer.subscribe(Collections.singletonList(TOPIC));

    return consumer;
  }

  private TomlConfig newConfiguration() {
    final TomlConfig configuration = new TomlConfig();
    configuration.producer.servers =
        Collections.singletonList(String.format("localhost:%d", kafkaHelper.kafkaPort()));

    return configuration;
  }

  private void startKafkaBroker() throws Exception {
    final int port = InstanceSpec.getRandomPort();
    final Properties properties = new Properties();
    kafkaConfigurator.accept(properties, port);
    kafkaBroker = EphemeralKafkaBroker.create(port, ALLOCATE_RANDOM_PORT, properties);
    kafkaHelper = KafkaHelper.createFor(kafkaBroker);
    kafkaBroker.start().join();
  }

  private void startZeebeBroker() {
    exporterConfiguration = newConfiguration();
    exporterConfigurator.accept(exporterConfiguration);
    exporterIntegrationRule = new ExporterIntegrationRule();
    exporterIntegrationRule.configure("kafka", KafkaExporter.class, exporterConfiguration);
    exporterIntegrationRule.start();
  }

  private static Map<String, Object> newBrokerSslConfiguration(int port) {
    final Map<String, Object> config = newSslConfiguration();
    config.put("listeners", "SSL://localhost:" + port);
    config.put("advertised.listeners", "SSL://localhost:" + port);
    config.put("security.inter.broker.protocol", "SSL");

    return config;
  }

  private static Map<String, Object> newSslConfiguration() {
    final Map<String, Object> config = new HashMap<>();
    config.put("security.protocol", "SSL");
    config.put("ssl.truststore.location", KEYSTORE_LOCATION);
    config.put("ssl.truststore.password", "test1234");
    config.put("ssl.keystore.location", KEYSTORE_LOCATION);
    config.put("ssl.keystore.password", "test1234");
    config.put("ssl.key.password", "test1234");

    return config;
  }

  private static Consumer<TomlConfig> exporter(Consumer<TomlConfig> configurator) {
    return configurator;
  }

  private static BiConsumer<Properties, Integer> kafka(
      BiConsumer<Properties, Integer> configurator) {
    return configurator;
  }
}
