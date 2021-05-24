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
package io.zeebe.exporters.kafka.serde;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.camunda.zeebe.protocol.record.Record;
import java.util.Objects;

/**
 * {@link RecordId} represents a unique identifier for a given Zeebe {@link
 * io.camunda.zeebe.protocol.record.Record}. On a single partition (identified via {@link
 * Record#getPartitionId()}), every record has a unique position (identified via {@link
 * Record#getPosition()}).
 */
public final class RecordId {
  @JsonProperty("partitionId")
  private final int partitionId;

  @JsonProperty("position")
  private final long position;

  @JsonCreator
  public RecordId(
      final @JsonProperty("partitionId") int partitionId,
      final @JsonProperty("position") long position) {
    this.partitionId = partitionId;
    this.position = position;
  }

  @JsonGetter
  public int getPartitionId() {
    return partitionId;
  }

  @JsonGetter
  public long getPosition() {
    return position;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPartitionId(), getPosition());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final RecordId recordId = (RecordId) o;
    return getPartitionId() == recordId.getPartitionId() && getPosition() == recordId.getPosition();
  }

  @Override
  public String toString() {
    return "RecordId{" + "partitionId=" + partitionId + ", position=" + position + '}';
  }
}
