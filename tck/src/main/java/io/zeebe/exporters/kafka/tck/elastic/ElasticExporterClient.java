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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.protocol.record.Record;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.elasticsearch.client.RestClient;

/**
 * Can stream back records that were exported to Elasticsearch using the official Elasticsearch
 * exporter.
 */
@SuppressWarnings("java:S1452")
public final class ElasticExporterClient implements AutoCloseable {
  private final RestClient client;
  private final String endpoint;

  public ElasticExporterClient(final RestClient client, final String indexPrefix) {
    this.client = client;

    final var indexFilter = URLEncoder.encode(indexPrefix, StandardCharsets.UTF_8);
    this.endpoint = String.format("/%s/_search", indexFilter);
  }

  public static ElasticExporterClientBuilder builder() {
    return new ElasticExporterClientBuilder();
  }

  @Override
  public void close() throws IOException {
    client.close();
  }

  /**
   * Returns a stream of records exported to Elasticsearch. As records are queried in batches, it's
   * possible records exported after the initial call will be returned in the stream. The stream
   * ends when there are no new records to read.
   *
   * @return a stream of records from Elasticsearch
   */
  public Stream<Record<?>> streamRecords() {
    return streamRecords(new SearchRequestDto(SearchIterator.SEARCH_SIZE));
  }

  /**
   * Returns a stream of records exported to Elasticsearch. As records are queried in batches, it's
   * possible records exported after the initial call will be returned in the stream. The stream
   * ends when there are no new records to read.
   *
   * @param lastAcknowledgedRecord the record to use as lower bound when fetching new records; the
   *     stream will return records that were exported logically after this record
   * @return a stream of records from Elasticsearch
   */
  public Stream<Record<?>> streamRecords(final @NonNull Record<?> lastAcknowledgedRecord) {
    final SearchRequestDto initialRequest = new SearchRequestDto(SearchIterator.SEARCH_SIZE);
    initialRequest.setSearchCursor(lastAcknowledgedRecord);

    return streamRecords(initialRequest);
  }

  private Stream<Record<?>> streamRecords(final SearchRequestDto initialRequest) {
    final SearchIterator iterator = new SearchIterator(client, endpoint, initialRequest);
    final Spliterator<Record<?>> spliterator =
        Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.SORTED);
    return StreamSupport.stream(spliterator, false);
  }
}
