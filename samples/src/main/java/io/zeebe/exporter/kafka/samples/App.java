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
package io.zeebe.exporter.kafka.samples;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
  private static final ObjectWriter JSON_WRITER =
      new ObjectMapper()
          .writerFor(new TypeReference<Map<String, Object>>() {})
          .withDefaultPrettyPrinter();

  public static void main(String[] args) throws JsonProcessingException {
    final String kafkaServer = args.length > 0 ? args[0] : "0.0.0.0:9093";
    final Logger logger = LoggerFactory.getLogger(App.class);
    final Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Collections.singletonList(kafkaServer));
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "zeebe");
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

    final Consumer<RecordKey, Map<String, Object>> consumer =
        new KafkaConsumer<>(config, new RecordKeyDeserializer(), new RecordDeserializer());
    consumer.subscribe(Pattern.compile("^zeebe.*$"));
    while (true) {
      final ConsumerRecords<RecordKey, Map<String, Object>> consumed =
          consumer.poll(Duration.ofSeconds(5));
      for (ConsumerRecord<RecordKey, Map<String, Object>> record : consumed) {
        logger.info(
            "================[{}] {}-{} ================",
            record.topic(),
            record.key().getPartitionId(),
            record.key().getPosition());
        logger.info("{}", JSON_WRITER.writeValueAsString(record.value()));
      }
    }
  }
}
