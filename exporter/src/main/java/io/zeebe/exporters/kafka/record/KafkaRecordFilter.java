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
package io.zeebe.exporters.kafka.record;

import io.camunda.zeebe.exporter.api.context.Context.RecordFilter;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link KafkaRecordFilter} is an implementation of {@link RecordFilter} which uses the {@link
 * RecordsConfig} to build the filter.
 */
public final class KafkaRecordFilter implements RecordFilter {
  private final RecordsConfig config;

  public KafkaRecordFilter(final RecordsConfig config) {
    this.config = Objects.requireNonNull(config);
  }

  /**
   * If any of the {@link RecordsConfig#getTypeMap()} accept the given record type, the {@code
   * recordType} is accepted.
   *
   * @param recordType {@inheritDoc}
   * @return {@inheritDoc}
   */
  @Override
  public boolean acceptType(final RecordType recordType) {
    return config.getDefaults().getAllowedTypes().contains(recordType)
        || config.getTypeMap().values().stream()
            .anyMatch(c -> c.getAllowedTypes().contains(recordType));
  }

  /**
   * If the {@link io.zeebe.exporters.kafka.config.RecordConfig} instance stored in {@link
   * RecordsConfig#getTypeMap()} for {@code valueType} has any allowed type at all, the {@code
   * valueType} is accepted.
   *
   * @param valueType {@inheritDoc}
   * @return {@inheritDoc}
   */
  @Override
  public boolean acceptValue(final ValueType valueType) {
    return !Optional.ofNullable(config.getTypeMap().get(valueType))
        .orElse(config.getDefaults())
        .getAllowedTypes()
        .isEmpty();
  }
}
