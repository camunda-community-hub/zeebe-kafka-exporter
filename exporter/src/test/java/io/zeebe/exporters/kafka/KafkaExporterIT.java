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
import static org.assertj.core.api.Assertions.entry;

import com.github.charithe.kafka.EphemeralKafkaBroker;
import com.github.charithe.kafka.KafkaJunitRule;
import com.github.charithe.kafka.StartupMode;
import com.google.protobuf.Message;
import io.zeebe.exporter.proto.RecordTransformer;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.exporter.record.Record;
import io.zeebe.exporters.kafka.config.toml.TomlConfig;
import io.zeebe.exporters.kafka.serde.RecordIdDeserializer;
import io.zeebe.exporters.kafka.serde.generic.GenericRecord;
import io.zeebe.exporters.kafka.serde.generic.GenericRecordDeserializer;
import io.zeebe.exporters.kafka.serde.generic.GenericRecordSerializer;
import io.zeebe.test.exporter.ExporterIntegrationRule;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class KafkaExporterIT {
  private static final String TOPIC = "zeebe";
  private final GenericRecordSerializer keySerializer = new GenericRecordSerializer();
  private final GenericRecordSerializer valueSerializer = new GenericRecordSerializer();

  private final ExporterIntegrationRule exporterIntegrationRule = new ExporterIntegrationRule();
  private final KafkaJunitRule kafkaRule =
      new KafkaJunitRule(EphemeralKafkaBroker.create(), StartupMode.WAIT_FOR_STARTUP);

  @Rule
  public final RuleChain chain = RuleChain.outerRule(kafkaRule).around(exporterIntegrationRule);

  @Before
  public void setUp() {
    keySerializer.configure(new HashMap<>(), true);
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

    final KafkaConsumer<Schema.RecordId, GenericRecord> consumer =
        kafkaRule
            .helper()
            .createConsumer(
                new RecordIdDeserializer(), new GenericRecordDeserializer(), properties);
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
