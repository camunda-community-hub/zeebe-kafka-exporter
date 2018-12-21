/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package io.zeebe.exporter.kafka.config.raw;

import io.zeebe.exporter.kafka.config.ClientConfig;
import io.zeebe.util.DurationUtil;
import java.util.List;
import java.util.Map;

public class RawClientConfig {
  public String clientId;
  public String closeTimeout;
  public Map<String, String> config;
  public int maxConcurrentRequests = 3;
  public String requestTimeout;
  public List<String> servers;

  public ClientConfig parse() {
    final ClientConfig parsed = new ClientConfig();
    parsed.maxConcurrentRequests = maxConcurrentRequests;
    parsed.servers = servers;

    if (clientId != null) {
      parsed.clientId = clientId;
    }

    if (closeTimeout != null) {
      parsed.closeTimeout = DurationUtil.parse(closeTimeout);
    }

    if (requestTimeout != null) {
      parsed.requestTimeout = DurationUtil.parse(requestTimeout);
    }

    if (config != null) {
      parsed.config = config;
    }

    return parsed;
  }
}
