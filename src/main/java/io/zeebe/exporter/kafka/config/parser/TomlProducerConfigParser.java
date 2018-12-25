package io.zeebe.exporter.kafka.config.parser;

import io.zeebe.exporter.kafka.config.Parser;
import io.zeebe.exporter.kafka.config.ProducerConfig;
import io.zeebe.exporter.kafka.config.toml.TomlProducerConfig;
import io.zeebe.util.DurationUtil;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class TomlProducerConfigParser implements Parser<TomlProducerConfig, ProducerConfig> {
  public static final List<String> DEFAULT_SERVERS = Collections.singletonList("localhost:9092");
  public static final String DEFAULT_CLIENT_ID = "zeebe";
  public static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(20);
  public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(5);
  public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 3;

  @Override
  public ProducerConfig parse(TomlProducerConfig config) {
    final ProducerConfig parsed = new ProducerConfig();

    if (config.maxConcurrentRequests < 1) {
      parsed.maxConcurrentRequests = DEFAULT_MAX_CONCURRENT_REQUESTS;
    } else {
      parsed.maxConcurrentRequests = config.maxConcurrentRequests;
    }

    if (config.servers != null) {
      parsed.servers = config.servers;
    } else {
      config.servers = DEFAULT_SERVERS;
    }

    if (config.clientId != null) {
      parsed.clientId = config.clientId;
    } else {
      parsed.clientId = DEFAULT_CLIENT_ID;
    }

    if (config.closeTimeout != null) {
      parsed.closeTimeout = DurationUtil.parse(config.closeTimeout);
    } else {
      parsed.closeTimeout = DEFAULT_CLOSE_TIMEOUT;
    }

    if (config.requestTimeout != null) {
      parsed.requestTimeout = DurationUtil.parse(config.requestTimeout);
    } else {
      parsed.requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    }

    if (config.config != null) {
      parsed.config = config.config;
    }

    return parsed;
  }
}
