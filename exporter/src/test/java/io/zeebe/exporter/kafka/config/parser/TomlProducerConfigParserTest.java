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
package io.zeebe.exporter.kafka.config.parser;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.exporter.kafka.config.ProducerConfig;
import io.zeebe.exporter.kafka.config.toml.TomlProducerConfig;
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
            null);
  }

  @Test
  public void shouldParse() {
    // given
    final TomlProducerConfig config = new TomlProducerConfig();
    config.maxConcurrentRequests = 1;
    config.servers = Collections.singletonList("localhost:3000");
    config.clientId = "client";
    config.closeTimeout = "3s";
    config.requestTimeout = "3s";
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
}
