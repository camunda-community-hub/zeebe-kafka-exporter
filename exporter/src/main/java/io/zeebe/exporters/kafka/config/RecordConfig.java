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

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import java.util.Objects;
import java.util.Set;

/**
 * {@link RecordConfig} describes what the exporter should do with a record of a given {@link
 * io.camunda.zeebe.protocol.record.ValueType} - this is mapped via {@link RecordsConfig}, which
 * holds a map of {@link io.camunda.zeebe.protocol.record.ValueType} to {@link RecordConfig}.
 *
 * <p>For the {@link io.camunda.zeebe.protocol.record.ValueType} associated with this instance, only
 * records with a {@link Record#getRecordType()} which is included in {@code allowedTypes} will be
 * exported. An empty set of {@code allowedTypes} means nothing gets exported.
 */
public final class RecordConfig {
  private final Set<RecordType> allowedTypes;
  private final String topic;

  public RecordConfig(final Set<RecordType> allowedTypes, final String topic) {
    this.allowedTypes = Objects.requireNonNull(allowedTypes);
    this.topic = Objects.requireNonNull(topic);
  }

  public Set<RecordType> getAllowedTypes() {
    return allowedTypes;
  }

  public String getTopic() {
    return topic;
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowedTypes, topic);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final RecordConfig that = (RecordConfig) o;
    return Objects.equals(getAllowedTypes(), that.getAllowedTypes())
        && Objects.equals(getTopic(), that.getTopic());
  }

  @Override
  public String toString() {
    return "RecordConfig{" + "allowedTypes=" + allowedTypes + ", topic='" + topic + '\'' + '}';
  }
}
