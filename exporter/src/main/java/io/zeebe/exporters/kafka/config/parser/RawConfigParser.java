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
import io.zeebe.exporters.kafka.config.raw.RawConfig;
import io.zeebe.exporters.kafka.config.raw.RawProducerConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordsConfig;
import java.time.Duration;
import java.util.Objects;

/**
 * {@link RawConfigParser} parses a given {@link RawConfig} into a valid {@link Config} instance,
 * substituting sane defaults for missing properties.
 *
 * <p>You can inject your own {@code recordsConfigParser} and {@code producerConfig} implementations
 * to overwrite the parsing for nested types.
 */
public final class RawConfigParser implements ConfigParser<RawConfig, Config> {
  static final int DEFAULT_MAX_BATCH_SIZE = 100;
  static final Duration DEFAULT_FLUSH_INTERVAL_MS = Duration.ofSeconds(1);

  private final ConfigParser<RawRecordsConfig, RecordsConfig> recordsConfigParser;
  private final ConfigParser<RawProducerConfig, ProducerConfig> producerConfigParser;

  public RawConfigParser() {
    this(new RawRecordsConfigParser(), new RawProducerConfigParser());
  }

  RawConfigParser(
      final ConfigParser<RawRecordsConfig, RecordsConfig> recordsConfigParser,
      final ConfigParser<RawProducerConfig, ProducerConfig> producerConfigParser) {
    this.recordsConfigParser = Objects.requireNonNull(recordsConfigParser);
    this.producerConfigParser = Objects.requireNonNull(producerConfigParser);
  }

  @Override
  public Config parse(final RawConfig config) {
    Objects.requireNonNull(config);

    final ProducerConfig producerConfig =
        producerConfigParser.parse(config.producer, RawProducerConfig::new);
    final RecordsConfig recordsConfig =
        recordsConfigParser.parse(config.records, RawRecordsConfig::new);
    final Integer maxBatchSize = get(config.maxBatchSize, DEFAULT_MAX_BATCH_SIZE);
    final Duration flushInterval =
        get(config.flushIntervalMs, DEFAULT_FLUSH_INTERVAL_MS, Duration::ofMillis);

    return new Config(producerConfig, recordsConfig, maxBatchSize, flushInterval);
  }
}
