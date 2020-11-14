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
package io.zeebe.exporters.kafka.tck.elastic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.zeebe.protocol.immutables.record.ImmutableRecord;
import io.zeebe.protocol.record.Record;
import java.util.Objects;

/** Bare minimum representation of the documents returned by a {@link SearchResponseDto}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DocumentDto {
  @JsonProperty(value = "_index", required = true)
  private String index;

  @JsonProperty(value = "_id", required = true)
  private String id;

  @JsonProperty(value = "_source", required = true)
  private ImmutableRecord<?> record;

  @Nullable
  public String getIndex() {
    return index;
  }

  @Nullable
  public String getId() {
    return id;
  }

  @SuppressWarnings("java:S1452")
  @Nullable
  public Record<?> getRecord() {
    return record;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIndex(), getId());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final DocumentDto document = (DocumentDto) o;
    return Objects.equals(getIndex(), document.getIndex())
        && Objects.equals(getId(), document.getId())
        && Objects.equals(getRecord(), document.getRecord());
  }

  @Override
  public String toString() {
    return "Document{"
        + "index='"
        + index
        + '\''
        + ", id='"
        + id
        + '\''
        + ", record="
        + record
        + '}';
  }
}
