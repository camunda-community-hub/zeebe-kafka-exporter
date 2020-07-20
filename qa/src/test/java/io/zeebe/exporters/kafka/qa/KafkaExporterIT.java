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
package io.zeebe.exporters.kafka.qa;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.containers.ZeebeBrokerContainer;
import io.zeebe.containers.ZeebePort;
import io.zeebe.exporters.kafka.serde.RecordDeserializer;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.serde.RecordIdDeserializer;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.protocol.record.Assertions;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import io.zeebe.protocol.record.intent.DeploymentIntent;
import io.zeebe.protocol.record.value.DeploymentRecordValue;
import io.zeebe.protocol.record.value.deployment.ResourceType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

/**
 * This tests the deployment of the exporter into a Zeebe broker in a as-close-to-production way as
 * possible, by starting a Zeebe container and deploying the exporter as one normally would. As the
 * verification tools are somewhat limited at the moment, we only verify that some things are
 * exported; a full verification of the behaviour is still done in the main exporter module. Once
 * verification tools get better, all the verification should be moved into this module.
 */
public final class KafkaExporterIT {
  private static final Pattern TOPIC_SUBSCRIPTION_PATTERN = Pattern.compile("zeebe.*");

  private final KafkaContainer kafkaContainer = newKafkaContainer();
  private final ZeebeBrokerContainer zeebeContainer = newZeebeContainer();

  @Rule
  public final RuleChain ruleChain = RuleChain.outerRule(kafkaContainer).around(zeebeContainer);

  private ZeebeClient client;

  @Before
  public void setUp() {
    client = newClient();
  }

  @After
  public void tearDown() {
    if (client != null) {
      client.close();
      client = null;
    }
  }

  @Test
  public void shouldExportToKafka() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent("start")
            .serviceTask("task")
            .zeebeJobType("type")
            .endEvent()
            .done();

    // when
    final DeploymentEvent deploymentEvent =
        client.newDeployCommand().addWorkflowModel(process, "process.bpmn").send().join();

    // then
    final List<ConsumerRecord<RecordId, Record<?>>> exportedRecords = consumeAllExportedRecords();
    final ConsumedRecord<DeploymentRecordValue> exportedDeploymentEvent =
        exportedRecords.stream()
            .filter(e -> e.value().getValueType() == ValueType.DEPLOYMENT)
            .filter(r -> r.value().getIntent() == DeploymentIntent.CREATE)
            .map(r -> new ConsumedRecord<>(r.key(), (Record<DeploymentRecordValue>) r.value()))
            .findFirst()
            .orElseThrow();
    final Record<DeploymentRecordValue> exportedRecord = exportedDeploymentEvent.getRecord();
    Assertions.assertThat(exportedRecord)
        .hasPartitionId(exportedDeploymentEvent.getId().getPartitionId());
    Assertions.assertThat(exportedRecord)
        .hasPosition(exportedDeploymentEvent.getId().getPosition());
    Assertions.assertThat(exportedRecord).hasRecordType(RecordType.COMMAND);
    Assertions.assertThat(exportedRecord.getValue().getResources().get(0))
        .hasResourceType(ResourceType.BPMN_XML)
        .hasResourceName("process.bpmn")
        .hasResource(Bpmn.convertToString(process).getBytes());
  }

  private List<ConsumerRecord<RecordId, Record<?>>> consumeAllExportedRecords() {
    final List<ConsumerRecord<RecordId, Record<?>>> records = new ArrayList<>();
    final Duration timeout = Duration.ofSeconds(5);
    ConsumerRecords<RecordId, Record<?>> consumedRecords;

    do {
      try (Consumer<RecordId, Record<?>> consumer = newConsumer()) {
        consumedRecords = consumer.poll(timeout);
        consumedRecords.forEach(records::add);
      }
    } while (!consumedRecords.isEmpty());

    return records;
  }

  private Consumer<RecordId, Record<?>> newConsumer() {
    final Properties config = newConsumerConfig();
    final Consumer<RecordId, Record<?>> consumer =
        new KafkaConsumer<>(config, new RecordIdDeserializer(), new RecordDeserializer());
    consumer.subscribe(TOPIC_SUBSCRIPTION_PATTERN);

    return consumer;
  }

  private ZeebeClient newClient() {
    return ZeebeClient.newClientBuilder()
        .brokerContactPoint(zeebeContainer.getExternalAddress(ZeebePort.GATEWAY))
        .usePlaintext()
        .build();
  }

  private Properties newConsumerConfig() {
    final Properties properties = new Properties();
    properties.put("auto.offset.reset", "earliest");
    properties.put("bootstrap.servers", kafkaContainer.getBootstrapServers());
    properties.put("enable.auto.commit", "true");
    properties.put("group.id", this.getClass().getName());
    properties.put("max.poll.records", String.valueOf(Integer.MAX_VALUE));
    properties.put("metadata.max.age.ms", "500");

    return properties;
  }

  @SuppressWarnings("OctalInteger")
  private static ZeebeBrokerContainer newZeebeContainer() {
    final ZeebeBrokerContainer container =
        new ZeebeBrokerContainer(ZeebeClient.class.getPackage().getImplementationVersion());
    final MountableFile exporterJar =
        MountableFile.forClasspathResource("zeebe-kafka-exporter.jar", 0775);
    final MountableFile exporterConfig =
        MountableFile.forClasspathResource("kafka-exporter.yml", 0775);
    final String networkAlias = "zeebe";

    return container
        .withNetwork(Network.SHARED)
        .withNetworkAliases(networkAlias)
        .withEnv("ZEEBE_BROKER_NETWORK_HOST", "0.0.0.0")
        .withEnv("ZEEBE_BROKER_NETWORK_ADVERTISEDHOST", networkAlias)
        .withEnv("ZEEBE_BROKER_EXPORTERS_KAFKA_ARGS_PRODUCER_SERVERS", "kafka:9092")
        .withCopyFileToContainer(exporterJar, "/usr/local/zeebe/lib/zeebe-kafka-exporter.jar")
        .withCopyFileToContainer(exporterConfig, "/usr/local/zeebe/config/kafka-exporter.yml")
        .withEnv(
            "SPRING_CONFIG_ADDITIONAL_LOCATION", "file:/usr/local/zeebe/config/kafka-exporter.yml")
        .withLogConsumer(new Slf4jLogConsumer(newContainerLogger("zeebeContainer"), true));
  }

  private static KafkaContainer newKafkaContainer() {
    final KafkaContainer container = new KafkaContainer("5.5.1");
    return container
        .withEmbeddedZookeeper()
        .withNetwork(Network.SHARED)
        .withNetworkAliases("kafka")
        .withLogConsumer(new Slf4jLogConsumer(newContainerLogger("kafkaContainer"), true));
  }

  private static Logger newContainerLogger(final String containerName) {
    return LoggerFactory.getLogger(KafkaExporterIT.class.getName() + "." + containerName);
  }
}
