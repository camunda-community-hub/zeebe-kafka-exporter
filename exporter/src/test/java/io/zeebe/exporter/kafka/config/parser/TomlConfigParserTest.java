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
package io.zeebe.exporter.kafka.config.parser;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.exporter.kafka.config.Config;
import io.zeebe.exporter.kafka.config.ProducerConfig;
import io.zeebe.exporter.kafka.config.RecordsConfig;
import io.zeebe.exporter.kafka.config.toml.TomlConfig;
import io.zeebe.exporter.kafka.config.toml.TomlProducerConfig;
import io.zeebe.exporter.kafka.config.toml.TomlRecordsConfig;
import java.time.Duration;
import org.junit.Test;

public class TomlConfigParserTest {
  private final MockParser<TomlRecordsConfig, RecordsConfig> recordsConfigParser =
      new MockParser<>(new TomlRecordsConfigParser());
  private final MockParser<TomlProducerConfig, ProducerConfig> producerConfigParser =
      new MockParser<>(new TomlProducerConfigParser());
  private final TomlConfigParser parser =
      new TomlConfigParser(recordsConfigParser, producerConfigParser);

  @Test
  public void shouldUseDefaultValues() {
    // given
    final TomlConfig config = new TomlConfig();

    // when
    final Config parsed = parser.parse(config);

    // then
    assertThat(parsed.records).isEqualTo(recordsConfigParser.parse(new TomlRecordsConfig()));
    assertThat(parsed.producer).isEqualTo(producerConfigParser.parse(new TomlProducerConfig()));
    assertThat(parsed.maxInFlightRecords).isEqualTo(TomlConfigParser.DEFAULT_MAX_IN_FLIGHT_RECORDS);
    assertThat(parsed.awaitInFlightRecordTimeout)
        .isEqualTo(TomlConfigParser.DEFAULT_AWAIT_IN_FLIGHT_RECORD_TIMEOUT);
  }

  @Test
  public void shouldParse() {
    // given
    final TomlConfig config = new TomlConfig();
    final ProducerConfig producerConfig = new ProducerConfig();
    final RecordsConfig recordsConfig = new RecordsConfig();
    producerConfigParser.config = producerConfig;
    recordsConfigParser.config = recordsConfig;
    config.awaitInFlightRecordTimeout = "1s";
    config.maxInFlightRecords = 2;

    // when
    final Config parsed = parser.parse(config);

    // then
    assertThat(parsed.producer).isSameAs(producerConfig);
    assertThat(parsed.records).isSameAs(recordsConfig);
    assertThat(parsed.awaitInFlightRecordTimeout).isEqualTo(Duration.ofSeconds(1));
    assertThat(parsed.maxInFlightRecords).isEqualTo(2);
  }
}
