package io.zeebe.exporter.kafka.config.parser;

import java.util.function.Supplier;

@FunctionalInterface
public interface Parser<T, R> {
  R parse(T config);

  default R parse(T config, Supplier<T> defaultValue) {
    if (config == null) {
      config = defaultValue.get();
    }

    return parse(config);
  }
}
