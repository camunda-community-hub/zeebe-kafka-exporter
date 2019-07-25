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
package io.zeebe.exporters.kafka.config.parser;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.config.ProducerConfig;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.exporters.kafka.config.toml.TomlConfig;
import io.zeebe.exporters.kafka.config.toml.TomlProducerConfig;
import io.zeebe.exporters.kafka.config.toml.TomlRecordsConfig;
import java.time.Duration;
import org.junit.Test;

public class TomlConfigParserTest {
  private final MockConfigParser<TomlRecordsConfig, RecordsConfig> recordsConfigParser =
      new MockConfigParser<>(new TomlRecordsConfigParser());
  private final MockConfigParser<TomlProducerConfig, ProducerConfig> producerConfigParser =
      new MockConfigParser<>(new TomlProducerConfigParser());
  private final TomlConfigParser parser =
      new TomlConfigParser(recordsConfigParser, producerConfigParser);

  @Test
  public void shouldUseDefaultValues() {
    // given
    final TomlConfig config = new TomlConfig();

    // when
    final Config parsed = parser.parse(config);

    // then
    assertThat(parsed.getRecords()).isEqualTo(recordsConfigParser.parse(new TomlRecordsConfig()));
    assertThat(parsed.getProducer())
        .isEqualTo(producerConfigParser.parse(new TomlProducerConfig()));
    assertThat(parsed.getMaxInFlightRecords())
        .isEqualTo(TomlConfigParser.DEFAULT_MAX_IN_FLIGHT_RECORDS);
    assertThat(parsed.getInFlightRecordCheckInterval())
        .isEqualTo(TomlConfigParser.DEFAULT_IN_FLIGHT_RECORD_CHECK_INTERVAL);
  }

  @Test
  public void shouldParse() {
    // given
    final TomlConfig config = new TomlConfig();
    final ProducerConfig producerConfig = new ProducerConfig();
    final RecordsConfig recordsConfig = new RecordsConfig();
    producerConfigParser.config = producerConfig;
    recordsConfigParser.config = recordsConfig;
    config.maxInFlightRecords = 2;
    config.inFlightRecordCheckIntervalMs = 500L;

    // when
    final Config parsed = parser.parse(config);

    // then
    assertThat(parsed.getProducer()).isSameAs(producerConfig);
    assertThat(parsed.getRecords()).isSameAs(recordsConfig);
    assertThat(parsed.getMaxInFlightRecords()).isEqualTo(2);
    assertThat(parsed.getInFlightRecordCheckInterval()).isEqualTo(Duration.ofMillis(500));
  }
}
