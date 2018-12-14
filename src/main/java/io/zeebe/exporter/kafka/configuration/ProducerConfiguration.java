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
package io.zeebe.exporter.kafka.configuration;

import io.zeebe.exporter.kafka.RecordSerializer;
import io.zeebe.util.ByteValue;
import io.zeebe.util.DurationUtil;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.record.CompressionType;

public class ProducerConfiguration {
  private String batchSize = "64K";
  private String batchLinger = "100ms";
  private String compressionType = CompressionType.SNAPPY.name;
  private String requestTimeout = "5s";
  private Map<String, String> extra = new HashMap<>();

  public Properties newProperties() {
    final Properties properties = new Properties();

    properties.putAll(extra);
    properties.put(ProducerConfig.BATCH_SIZE_CONFIG, String.valueOf(getBatchSize().toBytes()));
    properties.put(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(getBatchLinger().toMillis()));
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, RecordSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RecordSerializer.class.getName());
    properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, getCompressionType().name);
    properties.put(
        ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(getRequestTimeout().toMillis()));

    return properties;
  }

  public ByteValue getBatchSize() {
    return new ByteValue(batchSize);
  }

  public void setBatchSize(String batchSize) {
    this.batchSize = batchSize;
  }

  public Duration getBatchLinger() {
    return DurationUtil.parse(batchLinger);
  }

  public void setBatchLinger(String batchLinger) {
    this.batchLinger = batchLinger;
  }

  public CompressionType getCompressionType() {
    return CompressionType.forName(compressionType);
  }

  public void setCompressionType(String compressionType) {
    this.compressionType = compressionType;
  }

  public Duration getRequestTimeout() {
    return DurationUtil.parse(requestTimeout);
  }

  public void setRequestTimeout(String requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public Map<String, String> getExtra() {
    return extra;
  }

  public void setExtra(Map<String, String> extra) {
    this.extra = extra;
  }

  @Override
  public String toString() {
    return "ProducerConfiguration{"
        + "batchSize='"
        + batchSize
        + '\''
        + ", batchLinger='"
        + batchLinger
        + '\''
        + ", compressionType='"
        + compressionType
        + '\''
        + ", requestTimeout='"
        + requestTimeout
        + '\''
        + ", extra="
        + extra
        + '}';
  }
}
