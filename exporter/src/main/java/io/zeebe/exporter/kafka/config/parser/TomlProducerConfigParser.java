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

import io.zeebe.exporter.kafka.config.ProducerConfig;
import io.zeebe.exporter.kafka.config.toml.TomlProducerConfig;
import io.zeebe.util.DurationUtil;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class TomlProducerConfigParser implements Parser<TomlProducerConfig, ProducerConfig> {
  public static final List<String> DEFAULT_SERVERS = Collections.singletonList("localhost:9092");
  public static final String DEFAULT_CLIENT_ID = "zeebe";
  public static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(20);
  public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(5);
  public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 3;

  @Override
  public ProducerConfig parse(TomlProducerConfig config) {
    final ProducerConfig parsed = new ProducerConfig();

    if (config.maxConcurrentRequests != null) {
      parsed.maxConcurrentRequests = config.maxConcurrentRequests;
    } else {
      parsed.maxConcurrentRequests = DEFAULT_MAX_CONCURRENT_REQUESTS;
    }

    if (config.servers != null) {
      parsed.servers = config.servers;
    } else {
      parsed.servers = DEFAULT_SERVERS;
    }

    if (config.clientId != null) {
      parsed.clientId = config.clientId;
    } else {
      parsed.clientId = DEFAULT_CLIENT_ID;
    }

    if (config.closeTimeout != null) {
      parsed.closeTimeout = DurationUtil.parse(config.closeTimeout);
    } else {
      parsed.closeTimeout = DEFAULT_CLOSE_TIMEOUT;
    }

    if (config.requestTimeout != null) {
      parsed.requestTimeout = DurationUtil.parse(config.requestTimeout);
    } else {
      parsed.requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    }

    if (config.config != null) {
      parsed.config = config.config;
    }

    return parsed;
  }
}
