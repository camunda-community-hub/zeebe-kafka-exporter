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
package io.zeebe.exporters.kafka.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link ProducerConfig} is used by instances of {@link
 * io.zeebe.exporters.kafka.producer.KafkaProducerFactory} to configure a producer. A few standard
 * configuration options were extracted as options (e.g. {@code clientId}, {@code servers}) as they
 * were common - everything else can be configured via the free-form {@code config} map.
 *
 * <p>NOTE: be aware the when configuring a producer using the {@code config} map, Kafka expects the
 * values to either be strings OR very specific data types. While these are well documented, if
 * you're unsure of the expected data type (e.g. Integer, Long, Boolean), then just pass a string
 * representation of what you want to use.
 */
public final class ProducerConfig {
  private final String clientId;
  private final Duration closeTimeout;
  private final Map<String, Object> config;
  private final Duration requestTimeout;
  private final Duration maxBlockingTimeout;
  private final List<String> servers;

  public ProducerConfig(
      final String clientId,
      final Duration closeTimeout,
      final Map<String, Object> config,
      final Duration requestTimeout,
      final Duration maxBlockingTimeout,
      final List<String> servers) {
    this.clientId = Objects.requireNonNull(clientId);
    this.closeTimeout = Objects.requireNonNull(closeTimeout);
    this.config = Objects.requireNonNull(config);
    this.requestTimeout = Objects.requireNonNull(requestTimeout);
    this.maxBlockingTimeout = Objects.requireNonNull(maxBlockingTimeout);
    this.servers = Objects.requireNonNull(servers);
  }

  public String getClientId() {
    return clientId;
  }

  public Duration getCloseTimeout() {
    return closeTimeout;
  }

  public Map<String, Object> getConfig() {
    return config;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public Duration getMaxBlockingTimeout() {
    return maxBlockingTimeout;
  }

  public List<String> getServers() {
    return servers;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        clientId, closeTimeout, config, requestTimeout, maxBlockingTimeout, servers);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ProducerConfig that = (ProducerConfig) o;
    return Objects.equals(getClientId(), that.getClientId())
        && Objects.equals(getCloseTimeout(), that.getCloseTimeout())
        && Objects.equals(getConfig(), that.getConfig())
        && Objects.equals(getRequestTimeout(), that.getRequestTimeout())
        && Objects.equals(getMaxBlockingTimeout(), that.getMaxBlockingTimeout())
        && Objects.equals(getServers(), that.getServers());
  }

  @Override
  public String toString() {
    return "ProducerConfig{"
        + "clientId='"
        + clientId
        + '\''
        + ", closeTimeout="
        + closeTimeout
        + ", config="
        + config
        + ", requestTimeout="
        + requestTimeout
        + ", maxBlockingTimeout="
        + maxBlockingTimeout
        + ", servers="
        + servers
        + '}';
  }
}
