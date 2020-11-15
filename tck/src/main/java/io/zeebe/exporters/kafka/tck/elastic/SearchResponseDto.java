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
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.List;

/** Bare minimum representation of a response from a search query. */
@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SearchResponseDto {
  @JsonProperty(value = "hits", required = true)
  private DocumentsWrapper documentsWrapper;

  /** @return the list of documents received; may be empty */
  @NonNull
  public List<DocumentDto> getDocuments() {
    return documentsWrapper.documents;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static final class DocumentsWrapper {
    @JsonProperty(value = "hits", required = true)
    private List<DocumentDto> documents = Collections.emptyList();
  }
}
