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
package io.zeebe.exporter.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.github.charithe.kafka.EphemeralKafkaBroker;
import com.github.charithe.kafka.KafkaJunitRule;
import com.github.charithe.kafka.StartupMode;
import io.zeebe.exporter.kafka.config.toml.TomlConfig;
import io.zeebe.exporter.kafka.record.RecordSerializer;
import io.zeebe.exporter.record.Record;
import io.zeebe.test.exporter.ExporterIntegrationRule;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class KafkaExporterIT {
  private static final String TOPIC = "zeebe";
  private final RecordSerializer keySerializer = new RecordSerializer();
  private final RecordSerializer valueSerializer = new RecordSerializer();

  private final ExporterIntegrationRule exporterIntegrationRule = new ExporterIntegrationRule();
  private final KafkaJunitRule kafkaRule =
      new KafkaJunitRule(EphemeralKafkaBroker.create(), StartupMode.WAIT_FOR_STARTUP);

  @Rule
  public final RuleChain chain = RuleChain.outerRule(kafkaRule).around(exporterIntegrationRule);

  @Before
  public void setUp() {
    keySerializer.configure(null, true);
    exporterIntegrationRule.configure("kafka", KafkaExporter.class, newConfiguration());
    exporterIntegrationRule.start();
  }

  @Test
  public void shouldExportRecords() {
    // when
    exporterIntegrationRule.performSampleWorkload();

    // then
    assertRecordsExported();
  }

  private void assertRecordsExported() {
    final Map<ByteBuffer, ByteBuffer> records = consumeAllExportedRecords();
    exporterIntegrationRule.visitExportedRecords(r -> assertRecordExported(records, r));
  }

  private void assertRecordExported(Map<ByteBuffer, ByteBuffer> producedRecords, Record<?> record) {
    assertThat(producedRecords)
        .contains(
            entry(
                ByteBuffer.wrap(keySerializer.serialize(TOPIC, record)),
                ByteBuffer.wrap(valueSerializer.serialize(TOPIC, record))));
  }

  private Map<ByteBuffer, ByteBuffer> consumeAllExportedRecords() {
    final Map<ByteBuffer, ByteBuffer> records = new HashMap<>();
    final Duration timeout = Duration.ofSeconds(5);

    try (KafkaConsumer<byte[], byte[]> consumer = newConsumer()) {
      consumer
          .poll(timeout)
          .forEach(r -> records.put(ByteBuffer.wrap(r.key()), ByteBuffer.wrap(r.value())));
    }

    return records;
  }

  private KafkaConsumer<byte[], byte[]> newConsumer() {
    final Properties properties = new Properties();
    properties.put("group.id", this.getClass().getName());
    properties.put("enable.auto.commit", "true");
    properties.put("auto.commit.interval.ms", "100");
    properties.put("max.poll.records", String.valueOf(Integer.MAX_VALUE));

    final KafkaConsumer<byte[], byte[]> consumer =
        kafkaRule
            .helper()
            .createConsumer(new ByteArrayDeserializer(), new ByteArrayDeserializer(), properties);
    consumer.subscribe(Collections.singletonList(TOPIC));

    return consumer;
  }

  private TomlConfig newConfiguration() {
    final TomlConfig configuration = new TomlConfig();
    configuration.producer.servers =
        Collections.singletonList(String.format("localhost:%d", kafkaRule.helper().kafkaPort()));

    return configuration;
  }
}
