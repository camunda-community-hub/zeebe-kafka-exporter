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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractConsumer {
  final Logger logger = LoggerFactory.getLogger("io.zeebe.exporters.kafka.samples.consumer");
  final Map<String, Object> config = new HashMap<>();

  void readConfig() {
    final String servers =
        System.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Arrays.asList(servers.split(",")));
    config.put(
        ConsumerConfig.GROUP_ID_CONFIG,
        System.getProperty(ConsumerConfig.GROUP_ID_CONFIG, "zeebe"));
    config.put(ConsumerConfig.CLIENT_ID_CONFIG, this.getClass().getName());
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
    config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 100);
    config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
    config.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 5000);
  }

  abstract void run();
}
