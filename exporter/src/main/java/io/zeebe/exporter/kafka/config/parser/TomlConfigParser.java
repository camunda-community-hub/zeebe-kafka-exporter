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

import io.zeebe.exporter.kafka.config.Config;
import io.zeebe.exporter.kafka.config.ProducerConfig;
import io.zeebe.exporter.kafka.config.RecordsConfig;
import io.zeebe.exporter.kafka.config.toml.TomlConfig;
import io.zeebe.exporter.kafka.config.toml.TomlProducerConfig;
import io.zeebe.exporter.kafka.config.toml.TomlRecordsConfig;
import io.zeebe.util.DurationUtil;
import java.time.Duration;

public class TomlConfigParser implements Parser<TomlConfig, Config> {
  public static final int DEFAULT_MAX_IN_FLIGHT_RECORDS = 3;
  public static final Duration DEFAULT_AWAIT_IN_FLIGHT_RECORD_TIMEOUT = Duration.ofSeconds(5);

  private final Parser<TomlRecordsConfig, RecordsConfig> recordsConfigParser;
  private final Parser<TomlProducerConfig, ProducerConfig> producerConfigParser;

  public TomlConfigParser() {
    this.recordsConfigParser = new TomlRecordsConfigParser();
    this.producerConfigParser = new TomlProducerConfigParser();
  }

  public TomlConfigParser(
      Parser<TomlRecordsConfig, RecordsConfig> recordsConfigParser,
      Parser<TomlProducerConfig, ProducerConfig> producerConfigParser) {
    this.recordsConfigParser = recordsConfigParser;
    this.producerConfigParser = producerConfigParser;
  }

  @Override
  public Config parse(TomlConfig config) {
    final Config parsed =
        new Config(
            producerConfigParser.parse(config.producer, TomlProducerConfig::new),
            recordsConfigParser.parse(config.records, TomlRecordsConfig::new));

    if (config.maxInFlightRecords != null) {
      parsed.maxInFlightRecords = config.maxInFlightRecords;
    } else {
      parsed.maxInFlightRecords = DEFAULT_MAX_IN_FLIGHT_RECORDS;
    }

    if (config.awaitInFlightRecordTimeout != null) {
      parsed.awaitInFlightRecordTimeout = DurationUtil.parse(config.awaitInFlightRecordTimeout);
    } else {
      parsed.awaitInFlightRecordTimeout = DEFAULT_AWAIT_IN_FLIGHT_RECORD_TIMEOUT;
    }

    return parsed;
  }
}
