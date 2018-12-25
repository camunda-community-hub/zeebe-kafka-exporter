package io.zeebe.exporter.kafka.config.parser;

import io.zeebe.exporter.kafka.config.Config;
import io.zeebe.exporter.kafka.config.Parser;
import io.zeebe.exporter.kafka.config.ProducerConfig;
import io.zeebe.exporter.kafka.config.RecordsConfig;
import io.zeebe.exporter.kafka.config.toml.TomlConfig;
import io.zeebe.exporter.kafka.config.toml.TomlProducerConfig;
import io.zeebe.exporter.kafka.config.toml.TomlRecordsConfig;
import io.zeebe.util.DurationUtil;

public class TomlConfigParser implements Parser<TomlConfig, Config> {
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
        producerConfigParser.parse(config.producer, ProducerConfig::new),
        recordsConfigParser.parse(config.records, RecordsConfig::new));

    if (config.maxInFlightRecords != null) {
      parsed.maxInFlightRecords = config.maxInFlightRecords;
    }

    if (config.awaitInFlightRecordTimeout != null) {
      parsed.awaitInFlightRecordTimeout = DurationUtil.parse(config.awaitInFlightRecordTimeout);
    }

    return parsed;
  }
}
