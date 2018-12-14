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
