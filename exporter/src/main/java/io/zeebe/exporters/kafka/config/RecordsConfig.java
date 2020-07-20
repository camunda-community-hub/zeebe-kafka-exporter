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
import io.zeebe.protocol.record.ValueType;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link RecordsConfig} provides a default {@link RecordConfig} for every {@link ValueType}, with
 * the possibility of setting a specific {@link RecordConfig} for a given {@link ValueType}.
 */
public final class RecordsConfig {
  private final Map<ValueType, RecordConfig> typeMap;
  private final RecordConfig defaults;

  public RecordsConfig(
      final @NonNull Map<ValueType, RecordConfig> typeMap, @NonNull final RecordConfig defaults) {
    this.typeMap = Objects.requireNonNull(typeMap);
    this.defaults = Objects.requireNonNull(defaults);
  }

  public @NonNull Map<ValueType, RecordConfig> getTypeMap() {
    return typeMap;
  }

  public @NonNull RecordConfig getDefaults() {
    return defaults;
  }

  /**
   * Returns the correct {@link RecordConfig} for this type, or {@link #getDefaults()} if none
   * defined for the given type.
   *
   * @param type the value type to get the {@link RecordConfig} of
   * @return the configured {@link RecordConfig} for this type, or {@link #getDefaults()}
   */
  public @NonNull RecordConfig forType(final @NonNull ValueType type) {
    return Optional.ofNullable(typeMap.get(type)).orElse(defaults);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaults, typeMap);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final RecordsConfig that = (RecordsConfig) o;
    return Objects.equals(getTypeMap(), that.getTypeMap())
        && Objects.equals(getDefaults(), that.getDefaults());
  }
}
