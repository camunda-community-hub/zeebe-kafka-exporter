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

import static io.zeebe.exporters.kafka.config.parser.ConfigParserUtil.get;

import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.config.ProducerConfig;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.exporters.kafka.config.toml.TomlConfig;
import io.zeebe.exporters.kafka.config.toml.TomlProducerConfig;
import io.zeebe.exporters.kafka.config.toml.TomlRecordsConfig;
import java.time.Duration;

public class TomlConfigParser implements ConfigParser<TomlConfig, Config> {
  static final int DEFAULT_MAX_IN_FLIGHT_RECORDS = 3;
  static final Duration DEFAULT_IN_FLIGHT_RECORD_CHECK_INTERVAL = Duration.ofSeconds(1);

  private final ConfigParser<TomlRecordsConfig, RecordsConfig> recordsConfigParser;
  private final ConfigParser<TomlProducerConfig, ProducerConfig> producerConfigParser;

  public TomlConfigParser() {
    this.recordsConfigParser = new TomlRecordsConfigParser();
    this.producerConfigParser = new TomlProducerConfigParser();
  }

  TomlConfigParser(
      ConfigParser<TomlRecordsConfig, RecordsConfig> recordsConfigParser,
      ConfigParser<TomlProducerConfig, ProducerConfig> producerConfigParser) {
    this.recordsConfigParser = recordsConfigParser;
    this.producerConfigParser = producerConfigParser;
  }

  @Override
  public Config parse(TomlConfig config) {
    final Config parsed =
        new Config(
            producerConfigParser.parse(config.producer, TomlProducerConfig::new),
            recordsConfigParser.parse(config.records, TomlRecordsConfig::new));

    parsed.setMaxInFlightRecords(get(config.maxInFlightRecords, DEFAULT_MAX_IN_FLIGHT_RECORDS));
    parsed.setInFlightRecordCheckInterval(
        get(
            config.inFlightRecordCheckIntervalMs,
            DEFAULT_IN_FLIGHT_RECORD_CHECK_INTERVAL,
            Duration::ofMillis));

    return parsed;
  }
}
