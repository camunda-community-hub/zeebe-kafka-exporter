package io.zeebe.exporter.kafka.config.parser;

import io.zeebe.exporter.kafka.config.Config;
import io.zeebe.exporter.kafka.config.ProducerConfig;
import io.zeebe.exporter.kafka.config.RecordsConfig;
import io.zeebe.exporter.kafka.config.toml.TomlConfig;
import io.zeebe.exporter.kafka.config.toml.TomlProducerConfig;
import io.zeebe.exporter.kafka.config.toml.TomlRecordsConfig;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class TomlConfigParserTest {
  private final MockParser<TomlRecordsConfig, RecordsConfig> recordsConfigParser =
    new MockParser<>(new TomlRecordsConfigParser());
  private final MockParser<TomlProducerConfig, ProducerConfig> producerConfigParser =
    new MockParser<>(new TomlProducerConfigParser());
  private final TomlConfigParser parser = new TomlConfigParser(recordsConfigParser, producerConfigParser);

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
    assertThat(parsed.awaitInFlightRecordTimeout).isEqualTo(TomlConfigParser.DEFAULT_AWAIT_IN_FLIGHT_RECORD_TIMEOUT);
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
