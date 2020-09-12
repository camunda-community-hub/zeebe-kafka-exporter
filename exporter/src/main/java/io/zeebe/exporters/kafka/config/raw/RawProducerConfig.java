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
package io.zeebe.exporters.kafka.config.raw;

@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class RawProducerConfig {

  /**
   * Producer client identifier; maps to {@link
   * org.apache.kafka.clients.producer.ProducerConfig#CLIENT_ID_CONFIG}
   */
  public String clientId;

  /** Grace period when shutting down the producer in milliseconds. */
  public Long closeTimeoutMs;

  /**
   * Line-separated list of Java properties, e.g. the contents of a properties file. The resulting
   * map is passed verbatim as part of the {@link org.apache.kafka.clients.producer.ProducerConfig}.
   * You can use any of the properties defined there. This allows you to configure OAuth, SSL, SASL,
   * etc.
   *
   * <p>Be careful as this allows you to overwrite anything - e.g. key and value serializers - which
   * can break the exporter behaviour, so make sure to properly test your settings before deploying.
   */
  public String config;

  /**
   * Controls how long the producer will wait for a request to be acknowledged by the Kafka broker
   * before retrying it. Maps to ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG
   */
  public Long requestTimeoutMs;

  /**
   * The comma separated list of initial Kafka broker contact points. The format should be the same
   * one as the {@link org.apache.kafka.clients.producer.ProducerConfig} expects, i.e. "host:port".
   * Maps to ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
   */
  public String servers;

  /**
   * The serialisation format for the message value
   *
   * <p>default is "json" also supported is "protobuf"
   */
  public String format;
}
