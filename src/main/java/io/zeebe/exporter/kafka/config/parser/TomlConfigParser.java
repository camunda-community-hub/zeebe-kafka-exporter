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
