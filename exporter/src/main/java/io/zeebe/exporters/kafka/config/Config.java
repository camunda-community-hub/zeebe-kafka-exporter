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
package io.zeebe.exporters.kafka.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Duration;
import java.util.Objects;

/**
 * Entrypoint for the effective {@link io.zeebe.exporters.kafka.KafkaExporter} configuration. This
 * is what the exporter will use as final configuration. See {@link
 * io.zeebe.exporters.kafka.config.raw.RawConfig} and {@link
 * io.zeebe.exporters.kafka.config.parser.RawConfigParser} for more on how the external
 * configuration is parsed into an instance of this class.
 */
public class Config {
  private final ProducerConfig producer;
  private final RecordsConfig records;
  private final int maxBatchSize;
  private final Duration commitInterval;

  public Config(
      final @NonNull ProducerConfig producer,
      final @NonNull RecordsConfig records,
      final int maxBatchSize,
      final @NonNull Duration commitInterval) {
    this.producer = Objects.requireNonNull(producer);
    this.records = Objects.requireNonNull(records);
    this.maxBatchSize = maxBatchSize;
    this.commitInterval = Objects.requireNonNull(commitInterval);
  }

  public @NonNull ProducerConfig getProducer() {
    return producer;
  }

  public @NonNull RecordsConfig getRecords() {
    return records;
  }

  public int getMaxBatchSize() {
    return maxBatchSize;
  }

  public @NonNull Duration getCommitInterval() {
    return commitInterval;
  }

  @Override
  public int hashCode() {
    return Objects.hash(producer, records, maxBatchSize, commitInterval);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Config config = (Config) o;
    return getMaxBatchSize() == config.getMaxBatchSize()
        && Objects.equals(getProducer(), config.getProducer())
        && Objects.equals(getRecords(), config.getRecords())
        && Objects.equals(getMaxBatchSize(), config.getMaxBatchSize())
        && Objects.equals(getCommitInterval(), config.getCommitInterval());
  }

  @Override
  public String toString() {
    return "Config{"
        + "producer="
        + producer
        + ", records="
        + records
        + ", maxBatchSize="
        + maxBatchSize
        + ", commitInterval="
        + commitInterval
        + '}';
  }
}
