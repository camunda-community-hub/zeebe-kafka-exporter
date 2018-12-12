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
package io.zeebe.exporter.kafka;

import io.zeebe.exporter.record.Record;
import io.zeebe.util.DurationUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.record.CompressionType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaExporterConfiguration {
  private String topic;
  private List<String> servers = new ArrayList<>();
  private Duration requestTimeout = Duration.ofSeconds(5);
  private int batchSize = 1_000;
  private Duration batchLinger = Duration.ofSeconds(5);
  private CompressionType compressionType = CompressionType.SNAPPY;
  private Map<String, ?> clientProperties = new HashMap<>();

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public Map<String, ?> getClientProperties() {
    return clientProperties;
  }

  public void setClientProperties(Map<String, ?> clientProperties) {
    this.clientProperties = clientProperties;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public Duration getBatchLinger() {
    return batchLinger;
  }

  public void setBatchLinger(Duration batchLinger) {
    this.batchLinger = batchLinger;
  }

  public void setBatchLinger(String batchLinger) {
    this.batchLinger = DurationUtil.parse(batchLinger);
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public void setRequestTimeout(String requestTimeout) {
    this.requestTimeout = DurationUtil.parse(requestTimeout);
  }

  public CompressionType getCompressionType() {
    return compressionType;
  }

  public void setCompressionType(CompressionType compressionType) {
    this.compressionType = compressionType;
  }

  public void setCompressionType(String name) {
    this.compressionType = CompressionType.forName(name);
  }

  public void setCompressionType(int id) {
    this.compressionType = CompressionType.forId(id);
  }

  public List<String> getServers() {
    return servers;
  }

  public void setServers(List<String> servers) {
    this.servers = servers;
  }

  public Producer<Record, Record> newProducer() {
    return new KafkaProducer<Record, Record>(new KafkaExporterProperties(this));
  }
}
