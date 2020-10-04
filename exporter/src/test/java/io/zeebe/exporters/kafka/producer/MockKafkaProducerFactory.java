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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.exporters.kafka.config.Config;
import io.zeebe.exporters.kafka.serde.RecordId;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;

/**
 * A utility implementation to allow more control of the execution of the {@link
 * io.zeebe.exporters.kafka.KafkaExporter} in tests. Allows overriding the producer which will be
 * given to the exporter - if none given, it will create a {@link MockProducer} and memoize the
 * value.
 */
public class MockKafkaProducerFactory implements KafkaProducerFactory {
  public Supplier<MockProducer<RecordId, byte[]>> mockProducerSupplier;
  public MockProducer<RecordId, byte[]> mockProducer;
  public String producerId;

  public MockKafkaProducerFactory(
      final @NonNull Supplier<MockProducer<RecordId, byte[]>> mockProducerSupplier) {
    this.mockProducerSupplier = Objects.requireNonNull(mockProducerSupplier);
  }

  @Override
  public @NonNull Producer<RecordId, byte[]> newProducer(
      final @NonNull Config config, final @NonNull String producerId) {
    this.producerId = producerId;
    if (mockProducer == null || mockProducer.closed()) {
      mockProducer = mockProducerSupplier.get();
    }

    return mockProducer;
  }
}
