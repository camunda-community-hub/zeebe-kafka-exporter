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
import java.util.Objects;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

/** Convenience builder class for an {@link ElasticExporterClient}. */
public final class ElasticExporterClientBuilder {
  private static final String DEFAULT_INDEX_PREFIX = "zeebe-record*";

  private RestClient client;
  private RestClientBuilder builder;
  private String indexPrefix = DEFAULT_INDEX_PREFIX;

  /**
   * @param client the underlying HTTP client to use
   * @return this builder for chaining
   */
  public ElasticExporterClientBuilder withClient(final @NonNull RestClient client) {
    this.client = Objects.requireNonNull(client);
    return this;
  }

  /**
   * @param builder a builder for the underlying HTTP client to use
   * @return this builder for chaining
   */
  public ElasticExporterClientBuilder withClientBuilder(final @NonNull RestClientBuilder builder) {
    this.builder = Objects.requireNonNull(builder);
    return this;
  }

  /**
   * The {@link ElasticExporterClient} will search over all indexes with the given prefix.
   *
   * @param indexPrefix the prefix of Zeebe indexes to search over
   * @return this builder for chaining
   */
  public ElasticExporterClientBuilder withIndexPrefix(final @NonNull String indexPrefix) {
    this.indexPrefix = Objects.requireNonNull(indexPrefix);
    return this;
  }

  public ElasticExporterClient build() {
    if (client == null) {
      if (builder == null) {
        throw new IllegalArgumentException(
            "Expected a builder to be present if no client given, but neither was present");
      }

      client = builder.build();
    }

    if (indexPrefix == null || indexPrefix.isBlank()) {
      indexPrefix = DEFAULT_INDEX_PREFIX;
    }

    if (!indexPrefix.endsWith("*")) {
      indexPrefix += "*";
    }

    return new ElasticExporterClient(client, indexPrefix);
  }
}
