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
package at.phactum.zeebe.exporters.kafka.producer;

import at.phactum.zeebe.exporters.kafka.config.ProducerConfig;

import java.util.function.LongConsumer;
import org.slf4j.Logger;

/**
 * While this seems like overhead, it's the only way to inject the record batch type into the
 * exporter instance, as the exporter instance is created by the Zeebe broker using the
 * argument-less constructor. The other option would be via configuration, which would be more
 * overhead, but the right approach in the future if multiple types are available.
 *
 * <p>The primary goal of this and the {@link RecordBatch} interface are to ease unit testing.
 */
@FunctionalInterface
public interface RecordBatchFactory {

  RecordBatch newRecordBatch(
      final ProducerConfig config,
      final int maxBatchSize,
      final LongConsumer onFlushCallback,
      final Logger logger);

  static RecordBatchFactory defaultFactory() {
    return (config, maxBatchSize, onFlushCallback, logger) ->
        new BoundedTransactionalRecordBatch(
            config, maxBatchSize, onFlushCallback, logger, KafkaProducerFactory.defaultFactory());
  }
}
