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
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.exporters.kafka.serde.RecordDeserializer;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.serde.RecordIdDeserializer;
import io.zeebe.exporters.kafka.tck.DebugHttpExporterClient;
import io.zeebe.exporters.kafka.tck.ExporterTechnologyCompatibilityKit;
import io.zeebe.protocol.record.Record;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * This tests the deployment of the exporter into a Zeebe broker in a as-close-to-production way as
 * possible, by starting a Zeebe container and deploying the exporter as one normally would.
 *
 * <p>In order to verify certain properties - i.e. all records were exported correctly, order was
 * maintained on a per partition basis, etc. - we use the Elasticsearch Exporter, which is official
 * and trusted, to compare results.
 */
@Execution(ExecutionMode.SAME_THREAD)
final class KafkaExporterIT {
  private static final Pattern TOPIC_SUBSCRIPTION_PATTERN = Pattern.compile("zeebe.*");
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaExporterIT.class);

  private Network network;
  private KafkaContainer kafkaContainer;
  private ZeebeContainer zeebeContainer;

  private ZeebeClient client;

  @BeforeEach
  void setUp() {
    network = Network.newNetwork();
    kafkaContainer = newKafkaContainer();
    zeebeContainer = newZeebeContainer();

    final Stream<Startable> containers = Stream.of(kafkaContainer, zeebeContainer);
    Startables.deepStart(containers).join();

    client = newClient();
  }

  @AfterEach
  void tearDown() {
    if (client != null) {
      client.close();
    }

    // safely close as many containers as possible
    Stream.of(zeebeContainer, kafkaContainer)
        .filter(Objects::nonNull)
        .parallel()
        .forEach(
            container -> {
              try {
                container.stop();
              } catch (final Exception e) {
                LOGGER.error("Failed to stop container {}", container);
              }
            });

    if (network != null) {
      network.close();
    }
  }

  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  @Test
  void shouldExportToKafka() throws MalformedURLException {
    // given
    final URL debugHttpServerUrl =
        new URL(
            String.format(
                "http://%s:%d/records.json",
                zeebeContainer.getContainerIpAddress(), zeebeContainer.getMappedPort(8000)));
    final ExporterTechnologyCompatibilityKit tck =
        new ExporterTechnologyCompatibilityKit(
            client, new DebugHttpExporterClient((debugHttpServerUrl)));

    // when
    tck.performSampleWorkload();

    // then
    final List<Record<?>> records = consumeAllExportedRecords();
    tck.assertAllRecordsExported(records);
    tck.assertRecordsMaintainOrderPerPartition(records);
  }

  private List<Record<?>> consumeAllExportedRecords() {
    final List<Record<?>> records = new ArrayList<>();
    final Duration timeout = Duration.ofSeconds(5);
    ConsumerRecords<RecordId, Record<?>> consumedRecords;

    do {
      try (Consumer<RecordId, Record<?>> consumer = newConsumer()) {
        consumedRecords = consumer.poll(timeout);
        consumedRecords.forEach(r -> records.add(r.value()));
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
        .brokerContactPoint(zeebeContainer.getExternalGatewayAddress())
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
  private ZeebeContainer newZeebeContainer() {
    final DockerImageName zeebeImageName =
        DockerImageName.parse("camunda/zeebe")
            .withTag(ZeebeClient.class.getPackage().getImplementationVersion());
    final ZeebeContainer container = new ZeebeContainer(zeebeImageName.asCanonicalNameString());
    final MountableFile exporterJar =
        MountableFile.forClasspathResource("zeebe-kafka-exporter.jar", 0775);
    final MountableFile exporterConfig = MountableFile.forClasspathResource("exporters.yml", 0775);
    final String networkAlias = "zeebe";
    final Slf4jLogConsumer logConsumer =
        new Slf4jLogConsumer(newContainerLogger("zeebeContainer"), true);

    container.addExposedPort(8000);
    return container
        .withNetwork(network)
        .withNetworkAliases(networkAlias)
        .withEnv("ZEEBE_BROKER_NETWORK_ADVERTISEDHOST", networkAlias)
        .withEnv("ZEEBE_BROKER_EXPORTERS_KAFKA_ARGS_PRODUCER_SERVERS", "kafka:9092")
        .withCopyFileToContainer(exporterJar, "/usr/local/zeebe/lib/zeebe-kafka-exporter.jar")
        .withCopyFileToContainer(exporterConfig, "/usr/local/zeebe/config/exporters.yml")
        .withEnv("SPRING_CONFIG_ADDITIONAL_LOCATION", "file:/usr/local/zeebe/config/exporters.yml")
        .withLogConsumer(logConsumer);
  }

  private KafkaContainer newKafkaContainer() {
    final DockerImageName kafkaImage =
        DockerImageName.parse("confluentinc/cp-kafka").withTag("5.5.1");
    final KafkaContainer container = new KafkaContainer(kafkaImage);
    final Slf4jLogConsumer logConsumer =
        new Slf4jLogConsumer(newContainerLogger("kafkaContainer"), true);

    return container
        .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
        .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
        .withEmbeddedZookeeper()
        .withNetwork(network)
        .withNetworkAliases("kafka")
        .withLogConsumer(logConsumer);
  }

  private static Logger newContainerLogger(final String containerName) {
    return LoggerFactory.getLogger(KafkaExporterIT.class.getName() + "." + containerName);
  }
}
