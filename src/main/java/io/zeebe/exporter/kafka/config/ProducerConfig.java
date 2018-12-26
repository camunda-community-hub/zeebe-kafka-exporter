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
package io.zeebe.exporter.kafka.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProducerConfig {
  public String clientId;
  public Duration closeTimeout;
  public Map<String, String> config;
  public int maxConcurrentRequests;
  public Duration requestTimeout;
  public List<String> servers;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProducerConfig)) return false;

    final ProducerConfig that = (ProducerConfig) o;
    return maxConcurrentRequests == that.maxConcurrentRequests
      && Objects.equals(clientId, that.clientId)
      && Objects.equals(closeTimeout, that.closeTimeout)
      && Objects.equals(config, that.config)
      && Objects.equals(requestTimeout, that.requestTimeout)
      && Objects.equals(servers, that.servers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      clientId, closeTimeout, config, maxConcurrentRequests, requestTimeout, servers);
  }
}
