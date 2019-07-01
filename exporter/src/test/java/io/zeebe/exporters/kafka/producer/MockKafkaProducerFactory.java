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
package io.zeebe.exporters.kafka.producer;

import io.zeebe.exporter.api.record.Record;
import io.zeebe.exporters.kafka.config.Config;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;

public class MockKafkaProducerFactory implements KafkaProducerFactory {
  public MockProducer<Record, Record> mockProducer;

  @Override
  public Producer<Record, Record> newProducer(Config config) {
    if (mockProducer == null) {
      mockProducer = new MockProducer<>();
    }

    return mockProducer;
  }
}
