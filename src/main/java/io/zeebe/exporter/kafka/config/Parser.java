package io.zeebe.exporter.kafka.config;

import java.util.function.Supplier;

@FunctionalInterface
public interface Parser<T, R> {
  R parse(T config);

  default R parse(T config, Supplier<R> defaultValue) {
    if (config == null) {
      return defaultValue.get();
    }

    return parse(config);
  }
}
