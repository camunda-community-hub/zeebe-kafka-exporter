package io.zeebe.exporter.kafka.config.parser;

public class MockParser<T, R> implements Parser<T, R> {
  private final Parser<T, R> defaultParser;
  public R config;

  public MockParser(Parser<T, R> defaultParser) {
    this.defaultParser = defaultParser;
  }

  @Override
  public R parse(T config) {
    if (this.config == null) {
      return defaultParser.parse(config);
    }

    return this.config;
  }
}
