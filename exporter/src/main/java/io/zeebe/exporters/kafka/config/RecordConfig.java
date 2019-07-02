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

import io.zeebe.protocol.record.RecordType;
import java.util.Objects;
import java.util.Set;

public class RecordConfig {
  private Set<RecordType> allowedTypes;
  private String topic;

  public Set<RecordType> getAllowedTypes() {
    return allowedTypes;
  }

  public void setAllowedTypes(Set<RecordType> allowedTypes) {
    this.allowedTypes = allowedTypes;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowedTypes, topic);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof RecordConfig)) {
      return false;
    }

    final RecordConfig that = (RecordConfig) o;
    return Objects.equals(allowedTypes, that.allowedTypes) && Objects.equals(topic, that.topic);
  }
}
