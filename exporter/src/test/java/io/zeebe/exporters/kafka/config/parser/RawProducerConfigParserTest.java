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

import io.zeebe.exporters.kafka.config.ProducerConfig;
import io.zeebe.exporters.kafka.config.raw.RawProducerConfig;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class RawProducerConfigParserTest {
  private final RawProducerConfigParser parser = new RawProducerConfigParser();

  @Test
  public void shouldUseDefaultValuesForMissingProperties() {
    // given
    final RawProducerConfig config = new RawProducerConfig();

    // when
    final ProducerConfig parsed = parser.parse(config);

    // then
    assertThat(parsed)
        .extracting(
            "servers",
            "clientId",
            "closeTimeout",
            "requestTimeout",
            "config")
        .containsExactly(
            RawProducerConfigParser.DEFAULT_SERVERS,
            RawProducerConfigParser.DEFAULT_CLIENT_ID,
            RawProducerConfigParser.DEFAULT_CLOSE_TIMEOUT,
            RawProducerConfigParser.DEFAULT_REQUEST_TIMEOUT,
            new HashMap<>());
  }

  @Test
  public void shouldParse() {
    // given
    final RawProducerConfig config = new RawProducerConfig();
    config.servers = "localhost:3000";
    config.clientId = "client";
    config.closeTimeoutMs = 3000L;
    config.requestTimeoutMs = 3000L;
    config.config = "linger.ms=5\nmax.buffer.count=2";

    // when
    final ProducerConfig parsed = parser.parse(config);

    // then
    assertThat(parsed)
        .extracting(
            "servers",
            "clientId",
            "closeTimeout",
            "requestTimeout",
            "config")
        .containsExactly(
            Collections.singletonList("localhost:3000"),
            "client",
            Duration.ofSeconds(3),
            Duration.ofSeconds(3),
            Map.of("linger.ms", "5", "max.buffer.count", "2"));
  }
}
