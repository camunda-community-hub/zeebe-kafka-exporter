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
package io.zeebe.exporters.kafka.samples;

import io.zeebe.exporter.proto.Schema;
import io.zeebe.exporters.kafka.serde.RecordIdDeserializer;
import io.zeebe.exporters.kafka.serde.SchemaDeserializer;
import java.time.Duration;
import java.util.Collections;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

@SuppressWarnings("ALL")
public class SingleTopicConsumer extends AbstractConsumer {
  public static void main(String[] args) {
    new SingleTopicConsumer().run();
  }

  public void run() {
    readConfig();

    final Consumer<Schema.RecordId, Schema.WorkflowInstanceRecord> consumer =
        new KafkaConsumer<>(
            config,
            new RecordIdDeserializer(),
            new SchemaDeserializer<>(Schema.WorkflowInstanceRecord.parser()));
    consumer.subscribe(Collections.singleton("zeebe-workflow"));
    while (true) {
      final ConsumerRecords<Schema.RecordId, Schema.WorkflowInstanceRecord> consumed =
          consumer.poll(Duration.ofSeconds(2));
      for (ConsumerRecord<Schema.RecordId, Schema.WorkflowInstanceRecord> record : consumed) {
        logger.info(
            "================[{}] {}-{} ================",
            record.topic(),
            record.key().getPartitionId(),
            record.key().getPosition());
        logger.info("{}", record.value().toString());
      }
    }
  }
}
