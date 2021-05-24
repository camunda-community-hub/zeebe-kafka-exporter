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
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.config.raw.RawProducerConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordsConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
final class RawConfigParserTest {
  private final MockConfigParser<RawRecordsConfig, RecordsConfig> recordsConfigParser =
      new MockConfigParser<>(new RawRecordsConfigParser());
  private final MockConfigParser<RawProducerConfig, ProducerConfig> producerConfigParser =
      new MockConfigParser<>(new RawProducerConfigParser());
  private final RawConfigParser parser =
      new RawConfigParser(recordsConfigParser, producerConfigParser);

  @Test
  void shouldUseDefaultValues() {
    // given
    final RawConfig config = new RawConfig();

    // when
    final Config parsed = parser.parse(config);

    // then
    assertThat(parsed.getRecords()).isEqualTo(recordsConfigParser.parse(new RawRecordsConfig()));
    assertThat(parsed.getProducer()).isEqualTo(producerConfigParser.parse(new RawProducerConfig()));
    assertThat(parsed.getMaxBatchSize()).isEqualTo(RawConfigParser.DEFAULT_MAX_BATCH_SIZE);
    assertThat(parsed.getFlushInterval()).isEqualTo(RawConfigParser.DEFAULT_FLUSH_INTERVAL_MS);
  }

  @Test
  void shouldParse() {
    // given
    final RawConfig config = new RawConfig();
    final ProducerConfig producerConfig = producerConfigParser.parse(new RawProducerConfig());
    final RecordsConfig recordsConfig = recordsConfigParser.parse(new RawRecordsConfig());
    config.maxBatchSize = 2;
    config.flushIntervalMs = 500L;

    // when
    final Config parsed = parser.parse(config);

    // then
    assertThat(parsed.getProducer()).isEqualTo(producerConfig);
    assertThat(parsed.getRecords()).isEqualTo(recordsConfig);
    assertThat(parsed.getMaxBatchSize()).isEqualTo(2);
    assertThat(parsed.getFlushInterval()).isEqualTo(Duration.ofMillis(500));
  }
}
