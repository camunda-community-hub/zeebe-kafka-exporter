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
package io.zeebe.exporter.kafka.config;

import java.time.Duration;
import java.util.Objects;

public class Config {
  public final ProducerConfig producer;
  public final RecordsConfig records;

  public int maxInFlightRecords = 1_000;
  public Duration awaitInFlightRecordTimeout = Duration.ofSeconds(5);

  public Config() {
    this(new ProducerConfig(), new RecordsConfig());
  }

  public Config(ProducerConfig producer, RecordsConfig records) {
    this.producer = producer;
    this.records = records;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof Config)) {
      return false;
    }

    final Config config = (Config) o;
    return maxInFlightRecords == config.maxInFlightRecords
        && Objects.equals(producer, config.producer)
        && Objects.equals(records, config.records)
        && Objects.equals(awaitInFlightRecordTimeout, config.awaitInFlightRecordTimeout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(producer, records, maxInFlightRecords, awaitInFlightRecordTimeout);
  }
}
