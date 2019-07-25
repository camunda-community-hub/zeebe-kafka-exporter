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

import static io.zeebe.exporters.kafka.config.parser.ConfigParserUtil.get;

import io.zeebe.exporters.kafka.config.ProducerConfig;
import io.zeebe.exporters.kafka.config.toml.TomlProducerConfig;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TomlProducerConfigParser implements ConfigParser<TomlProducerConfig, ProducerConfig> {
  static final List<String> DEFAULT_SERVERS = Collections.singletonList("localhost:9092");
  static final String DEFAULT_CLIENT_ID = "zeebe";
  static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(20);
  static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(5);
  static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 3;

  @Override
  public ProducerConfig parse(TomlProducerConfig config) {
    final ProducerConfig parsed = new ProducerConfig();

    parsed.setMaxConcurrentRequests(
        get(config.maxConcurrentRequests, DEFAULT_MAX_CONCURRENT_REQUESTS));
    parsed.setServers(get(config.servers, DEFAULT_SERVERS));
    parsed.setClientId(get(config.clientId, DEFAULT_CLIENT_ID));
    parsed.setCloseTimeout(get(config.closeTimeoutMs, DEFAULT_CLOSE_TIMEOUT, Duration::ofMillis));
    parsed.setRequestTimeout(
        get(config.requestTimeoutMs, DEFAULT_REQUEST_TIMEOUT, Duration::ofMillis));
    parsed.setConfig(get(config.config, Collections.emptyMap(), this::parseConfig));

    return parsed;
  }

  /**
   * The TOML parser used by the Zeebe broker returns quoted keys with their original quotes, which
   * must be stripped in order for the {@link ProducerConfig} to accept them.
   *
   * @param original the original map provided by the TOML parser
   * @return map of properties ProducerConfig can accept
   */
  private Map<String, Object> parseConfig(Map<String, Object> original) {
    final Map<String, Object> parsed = new HashMap<>(original.size());
    for (Map.Entry<String, Object> entry : original.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("\"") && key.endsWith("\"")) {
        key = key.substring(1, key.length() - 1);
      }

      parsed.put(key, entry.getValue());
    }

    return parsed;
  }
}
