package io.zeebe.exporter.kafka.config;

import io.zeebe.exporter.kafka.config.parser.TomlConfigParser;
import io.zeebe.exporter.kafka.config.toml.TomlConfig;

public class MockConfigParser implements Parser<TomlConfig, Config> {
  private final TomlConfigParser defaultParser = new TomlConfigParser();
  public Config config;

  @Override
  public Config parse(TomlConfig config) {
    if (this.config == null) {
      this.config = defaultParser.parse(config);
    }

    return this.config;
  }
}
