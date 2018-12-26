package io.zeebe.exporter.kafka.config.parser;

import io.zeebe.exporter.kafka.config.ProducerConfig;
import io.zeebe.exporter.kafka.config.toml.TomlProducerConfig;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class TomlProducerConfigParserTest {
  private final TomlProducerConfigParser parser = new TomlProducerConfigParser();

  @Test
  public void shouldUseDefaultValuesForMissingProperties() {
    // given
    final TomlProducerConfig config = new TomlProducerConfig();

    // when
    final ProducerConfig parsed = parser.parse(config);

    // then
    assertThat(parsed)
      .extracting(
        "maxConcurrentRequests",
        "servers",
        "clientId",
        "closeTimeout",
        "requestTimeout",
        "config")
      .containsExactly(
        TomlProducerConfigParser.DEFAULT_MAX_CONCURRENT_REQUESTS,
        TomlProducerConfigParser.DEFAULT_SERVERS,
        TomlProducerConfigParser.DEFAULT_CLIENT_ID,
        TomlProducerConfigParser.DEFAULT_CLOSE_TIMEOUT,
        TomlProducerConfigParser.DEFAULT_REQUEST_TIMEOUT,
        null);
  }

  @Test
  public void shouldParse() {
    // given
    final TomlProducerConfig config = new TomlProducerConfig();
    config.maxConcurrentRequests = 1;
    config.servers = Collections.singletonList("localhost:3000");
    config.clientId = "client";
    config.closeTimeout = "3s";
    config.requestTimeout = "3s";
    config.config = new HashMap<>();

    // when
    final ProducerConfig parsed = parser.parse(config);

    // then
    assertThat(parsed)
      .extracting(
        "maxConcurrentRequests",
        "servers",
        "clientId",
        "closeTimeout",
        "requestTimeout",
        "config")
      .containsExactly(
        1,
        Collections.singletonList("localhost:3000"),
        "client",
        Duration.ofSeconds(3),
        Duration.ofSeconds(3),
        config.config);
  }
}
