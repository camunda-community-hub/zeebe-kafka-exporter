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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.protocol.record.Record;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.concurrent.NotThreadSafe;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class SearchIterator implements Iterator<Record<?>> {
  static final int SEARCH_SIZE = 100;
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchIterator.class);

  private final RestClient client;
  private final String endpoint;
  private final SearchRequestDto mutableSearchQuery;
  private final Deque<DocumentDto> recordsQueue;

  SearchIterator(final @NonNull RestClient client, final @NonNull String endpoint) {
    this(client, endpoint, new SearchRequestDto(SEARCH_SIZE));
  }

  SearchIterator(
      final @NonNull RestClient client,
      final @NonNull String endpoint,
      final @NonNull SearchRequestDto mutableSearchQuery) {
    this.client = Objects.requireNonNull(client);
    this.endpoint = Objects.requireNonNull(endpoint);
    this.mutableSearchQuery = Objects.requireNonNull(mutableSearchQuery);

    this.recordsQueue = new ArrayDeque<>(mutableSearchQuery.getSize());
  }

  @Override
  public boolean hasNext() {
    if (recordsQueue.isEmpty()) {
      fetchRecords();
    }

    return !recordsQueue.isEmpty();
  }

  @Override
  public Record<?> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    final DocumentDto document = recordsQueue.poll();
    if (document == null) {
      throw new NoSuchElementException();
    }

    return document.getRecord();
  }

  private void fetchRecords() {
    final Request request = new Request("POST", endpoint);

    try {
      request.setJsonEntity(MAPPER.writeValueAsString(mutableSearchQuery));
    } catch (final JsonProcessingException e) {
      LOGGER.error("Failed to serialize search query, aborting...", e);
      return;
    }

    try {
      search(request)
          .ifPresent(
              response -> {
                recordsQueue.addAll(response.getDocuments());
                final DocumentDto lastDocument = recordsQueue.peekLast();

                if (lastDocument != null) {
                  mutableSearchQuery.setSearchCursor(
                      Objects.requireNonNull(lastDocument.getRecord()));
                }
              });
    } catch (final JsonProcessingException e) {
      LOGGER.warn("Failed to parse search response, retrying...", e);
    } catch (final IOException e) {
      LOGGER.warn("Failed to perform search query against server, retrying...", e);
    }
  }

  @NonNull
  private Optional<SearchResponseDto> search(final @NonNull Request request) throws IOException {
    try {
      final Response response = client.performRequest(request);

      final SearchResponseDto searchResponseDto =
          MAPPER.readValue(response.getEntity().getContent(), SearchResponseDto.class);
      return Optional.of(searchResponseDto);
    } catch (final ResponseException e) {
      LOGGER.warn("Failed to execute search request {}", request, e);
    }

    return Optional.empty();
  }
}
