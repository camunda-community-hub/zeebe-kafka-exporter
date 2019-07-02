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
package io.zeebe.exporters.kafka.config.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.zeebe.exporters.kafka.config.ProducerConfig;
import io.zeebe.exporters.kafka.config.toml.TomlProducerConfig;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import org.junit.Test;

public class TomlProducerConfigParserTest {
  private final TomlProducerConfigParser parser = new TomlProducerConfigParser();

  @Test
  public void shouldUseDefaultValuesForMissingProperties() {
    // given
    final TomlProducerConfig config = new TomlProducerConfig();

    // when
    final ProducerConfig parsed = parser.parse(config);

    // then
    assertThat(parsed)
        .extracting(
            "maxConcurrentRequests",
            "servers",
            "clientId",
            "closeTimeout",
            "requestTimeout",
            "config")
        .containsExactly(
            TomlProducerConfigParser.DEFAULT_MAX_CONCURRENT_REQUESTS,
            TomlProducerConfigParser.DEFAULT_SERVERS,
            TomlProducerConfigParser.DEFAULT_CLIENT_ID,
            TomlProducerConfigParser.DEFAULT_CLOSE_TIMEOUT,
            TomlProducerConfigParser.DEFAULT_REQUEST_TIMEOUT,
            new HashMap<>());
  }

  @Test
  public void shouldParse() {
    // given
    final TomlProducerConfig config = new TomlProducerConfig();
    config.maxConcurrentRequests = 1;
    config.servers = Collections.singletonList("localhost:3000");
    config.clientId = "client";
    config.closeTimeoutMs = 3000L;
    config.requestTimeoutMs = 3000L;
    config.config = new HashMap<>();

    // when
    final ProducerConfig parsed = parser.parse(config);

    // then
    assertThat(parsed)
        .extracting(
            "maxConcurrentRequests",
            "servers",
            "clientId",
            "closeTimeout",
            "requestTimeout",
            "config")
        .containsExactly(
            1,
            Collections.singletonList("localhost:3000"),
            "client",
            Duration.ofSeconds(3),
            Duration.ofSeconds(3),
            config.config);
  }

  @Test
  public void shouldUnquoteConfigEntries() {
    // given
    final TomlProducerConfig config = new TomlProducerConfig();
    config.config = new HashMap<>();
    config.config.put("\"batch.size\"", 132000L);
    config.config.put("timeout", 5000);

    // when
    final ProducerConfig parsed = parser.parse(config);

    // then
    assertThat(parsed.getConfig())
        .containsOnly(entry("batch.size", 132000L), entry("timeout", 5000));
  }
}
