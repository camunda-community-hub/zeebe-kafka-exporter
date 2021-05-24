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
public final class RawProducerConfig {

  /**
   * Producer client identifier.
   *
   * @see org.apache.kafka.clients.producer.ProducerConfig#CLIENT_ID_CONFIG
   */
  public String clientId;

  /**
   * Grace period when shutting down the producer in milliseconds. A period which is too short could
   * result in possible resource leaks, but generally should be fine.
   */
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
   * before retrying it.
   *
   * @see org.apache.kafka.clients.producer.ProducerConfig#REQUEST_TIMEOUT_MS_CONFIG
   */
  public Long requestTimeoutMs;

  /**
   * The maximum time to block for all blocking requests, e.g. beginTransaction, commitTransaction.
   * It's recommended to keep this low, around a second, as it's also the time the exporter will
   * block if the batch is full when trying to commit/flush it. Keeping it low isn't a big issue, as
   * even if it times out the first time, Kafka will still commit the transaction in the background,
   * and on the next try the transaction will commit much faster (e.g. if it's already committed as
   * far as the brokers are concerned, then it should be really fast).
   *
   * @see org.apache.kafka.clients.producer.ProducerConfig#MAX_BLOCK_MS_CONFIG
   */
  public Long maxBlockingTimeoutMs;

  /**
   * The comma separated list of initial Kafka broker contact points. The format should be the same
   * one as the {@link org.apache.kafka.clients.producer.ProducerConfig} expects, i.e. "host:port".
   *
   * @see org.apache.kafka.clients.producer.ProducerConfig#BOOTSTRAP_SERVERS_CONFIG
   */
  public String servers;
}
