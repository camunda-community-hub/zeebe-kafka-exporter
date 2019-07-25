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

import io.zeebe.exporter.api.context.Context.RecordFilter;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import java.util.Optional;

public class KafkaRecordFilter implements RecordFilter {
  private final RecordsConfig config;

  public KafkaRecordFilter(RecordsConfig config) {
    this.config = config;
  }

  @Override
  public boolean acceptType(RecordType recordType) {
    return config
        .getTypeMap()
        .values()
        .stream()
        .anyMatch(c -> c.getAllowedTypes().contains(recordType));
  }

  @Override
  public boolean acceptValue(ValueType valueType) {
    return Optional.ofNullable(config.getTypeMap().get(valueType))
        .map(c -> !c.getAllowedTypes().isEmpty())
        .orElse(false);
  }
}
