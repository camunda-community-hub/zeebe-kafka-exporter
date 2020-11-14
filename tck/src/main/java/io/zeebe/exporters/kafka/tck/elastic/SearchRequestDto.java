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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.protocol.record.Record;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Bare minimum representation of a search request. */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
@JsonInclude(Include.NON_EMPTY)
public final class SearchRequestDto {

  private static final List<Map<String, String>> SORT_ORDER =
      List.of(Map.of("partitionId", "asc"), Map.of("position", "asc"));
  private static final List<String> STORED_FIELDS = List.of("_source", "_id");

  @JsonProperty("size")
  private final int size;

  @JsonProperty("sort")
  private final List<Map<String, String>> sortOptions;

  @JsonProperty("stored_fields")
  private final List<String> fields;

  @JsonProperty("search_after")
  private List<Object> searchCursor;

  public SearchRequestDto(final int size) {
    this.size = size;

    this.searchCursor = Collections.emptyList();
    this.sortOptions = SORT_ORDER;
    this.fields = STORED_FIELDS;
  }

  /**
   * @return the maximum number of documents that can be returned in the {@link SearchResponseDto}
   */
  public int getSize() {
    return size;
  }

  /**
   * This allows the same search request to be used repeatedly to iterate over the indexes in
   * batches by mutating the search cursor.
   *
   * @param record the record to use as lower bound when updating the search cursor
   */
  public void setSearchCursor(final @NonNull Record<?> record) {
    searchCursor = List.of(record.getPartitionId(), record.getPosition());
  }
}
