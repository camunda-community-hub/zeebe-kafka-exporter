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

import io.zeebe.exporter.kafka.serialization.RecordKeySerializer;
import io.zeebe.exporter.kafka.serialization.RecordSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class KafkaExporterProperties extends Properties {
  private final KafkaExporterConfiguration configuration;

  public KafkaExporterProperties(KafkaExporterConfiguration configuration) {
    this.configuration = configuration;

    setRequiredProperties();
    setCompression();
    setSerializers();
    setTimeouts();
    setExtraProperties();
  }

  protected void setExtraProperties() {
    putAll(configuration.getClientProperties());
  }

  protected void setRequiredProperties() {
    // Sets the known servers, a list of host:port pair, e.g. "localhost:3000,localhost:5000"
    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getServers());
  }

  protected void setSerializers() {
    // Use RecordKeySerializer as key serializer
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, RecordKeySerializer.class);
    // Use RecordSerializer as value serializer
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RecordSerializer.class);
  }

  protected void setCompression() {
    // Use snappy for compression
    put(ProducerConfig.COMPRESSION_TYPE_CONFIG, configuration.getCompressionType().toString());
  }

  protected void setTimeouts() {
    // Set a 5 second request timeout
    put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, configuration.getRequestTimeout().toMillis());
  }
}
