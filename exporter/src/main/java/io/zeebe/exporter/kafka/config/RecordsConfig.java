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

import io.zeebe.protocol.clientapi.ValueType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class RecordsConfig {
  public RecordConfig defaults;
  public final Map<ValueType, RecordConfig> typeMap = new EnumMap<>(ValueType.class);

  public RecordConfig forType(ValueType type) {
    return typeMap.get(type);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof RecordsConfig)) {
      return false;
    }

    final RecordsConfig that = (RecordsConfig) o;
    return Objects.equals(defaults, that.defaults) && Objects.equals(typeMap, that.typeMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaults, typeMap);
  }
}
