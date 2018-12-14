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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.charithe.kafka.KafkaJunitRule;
import io.zeebe.broker.system.configuration.ExporterCfg;
import io.zeebe.exporter.kafka.configuration.ExporterConfiguration;
import io.zeebe.test.EmbeddedBrokerRule;
import java.util.Collections;
import java.util.Map;

public class ZeebeRule extends EmbeddedBrokerRule {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @SuppressWarnings("unchecked")
  public ZeebeRule(KafkaJunitRule kafkaRule, ExporterConfiguration configuration) {
    super(
        c -> {
          configuration.setServers(
              Collections.singletonList(
                  String.format("localhost:%d", kafkaRule.helper().kafkaPort())));
          configuration.setTopic("zeebe");

          final ExporterCfg exporterCfg = new ExporterCfg();
          exporterCfg.setId("kafka");
          exporterCfg.setClassName(KafkaExporter.class.getCanonicalName());
          exporterCfg.setArgs(MAPPER.convertValue(configuration, Map.class));
          c.getExporters().add(exporterCfg);
        });
  }
}
