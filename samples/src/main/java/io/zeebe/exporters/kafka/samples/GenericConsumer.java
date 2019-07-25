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
import io.zeebe.exporters.kafka.serde.generic.GenericRecord;
import io.zeebe.exporters.kafka.serde.generic.GenericRecordDeserializer;
import java.time.Duration;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

@SuppressWarnings("InfiniteLoopStatement")
public class GenericConsumer extends AbstractConsumer {
  public static void main(String[] args) {
    new GenericConsumer().run();
  }

  public void run() {
    readConfig();

    final Consumer<Schema.RecordId, GenericRecord> consumer =
        new KafkaConsumer<>(config, new RecordIdDeserializer(), new GenericRecordDeserializer());
    consumer.subscribe(Pattern.compile("^zeebe-.*$"));
    while (true) {
      final ConsumerRecords<Schema.RecordId, GenericRecord> consumed =
          consumer.poll(Duration.ofSeconds(5));
      for (ConsumerRecord<Schema.RecordId, GenericRecord> record : consumed) {
        logger.info(
            "================[{}] {}-{} ================",
            record.topic(),
            record.key().getPartitionId(),
            record.key().getPosition());
        logger.info("{}", record.value().getMessage().toString());
      }
    }
  }
}
