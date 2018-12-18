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

import static io.zeebe.test.util.record.RecordingExporter.workflowInstanceRecords;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.github.charithe.kafka.EphemeralKafkaBroker;
import com.github.charithe.kafka.KafkaJunitRule;
import com.github.charithe.kafka.StartupMode;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.subscription.JobWorker;
import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.record.value.IncidentRecordValue;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.protocol.intent.IncidentIntent;
import io.zeebe.protocol.intent.WorkflowInstanceIntent;
import io.zeebe.test.util.TestUtil;
import io.zeebe.test.util.record.RecordingExporter;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class KafkaExporterIT {
  private static final String TOPIC = "zeebe";
  private KafkaExporterConfiguration configuration = new KafkaExporterConfiguration();
  private KafkaJunitRule kafkaRule =
      new KafkaJunitRule(EphemeralKafkaBroker.create(), StartupMode.WAIT_FOR_STARTUP);
  private ZeebeRule zeebeRule = new ZeebeRule(kafkaRule, configuration);

  @Rule public RuleChain chain = RuleChain.outerRule(kafkaRule).around(zeebeRule);

  private ZeebeClient zeebeClient;
  private KafkaConsumer<String, String> kafkaConsumer;

  public static final BpmnModelInstance WORKFLOW =
      Bpmn.createExecutableProcess("testProcess")
          .startEvent()
          .intermediateCatchEvent(
              "message", e -> e.message(m -> m.name("catch").zeebeCorrelationKey("$.orderId")))
          .serviceTask("task", t -> t.zeebeTaskType("work").zeebeTaskHeader("foo", "bar"))
          .endEvent()
          .done();

  public static final BpmnModelInstance SECOND_WORKFLOW =
      Bpmn.createExecutableProcess("secondProcess").startEvent().endEvent().done();

  @Before
  public void setUp() {
    kafkaConsumer = createConsumer();
    zeebeClient =
        ZeebeClient.newClientBuilder()
            .brokerContactPoint(zeebeRule.getGatewayAddress().toString())
            .build();
  }

  @After
  public void teardown() {
    if (zeebeClient != null) {
      zeebeClient.close();
      zeebeClient = null;
    }

    if (kafkaConsumer != null) {
      kafkaConsumer.close();
      kafkaConsumer = null;
    }
  }

  @Test
  public void shouldExportRecords() throws InterruptedException {
    final String orderId = "foo-bar-123";

    // deploy workflow
    zeebeClient
        .newDeployCommand()
        .addWorkflowModel(WORKFLOW, "workflow.bpmn")
        .addWorkflowModel(SECOND_WORKFLOW, "secondWorkflow.bpmn")
        .send()
        .join();

    // start instance
    zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("testProcess")
        .latestVersion()
        .payload(Collections.singletonMap("orderId", orderId))
        .send()
        .join();

    // create job worker which fails on first try and sets retries to 0 to create an incident
    final AtomicBoolean fail = new AtomicBoolean(true);

    final JobWorker worker =
        zeebeClient
            .newWorker()
            .jobType("work")
            .handler(
                (client, job) -> {
                  if (fail.getAndSet(false)) {
                    // fail job
                    client
                        .newFailCommand(job.getKey())
                        .retries(0)
                        .errorMessage("failed")
                        .send()
                        .join();
                  } else {
                    client.newCompleteCommand(job.getKey()).send().join();
                  }
                })
            .open();

    // publish message to trigger message catch event
    zeebeClient
        .newPublishMessageCommand()
        .messageName("catch")
        .correlationKey(orderId)
        .send()
        .join();

    // wait for incident
    final Record<IncidentRecordValue> incident =
        RecordingExporter.incidentRecords(IncidentIntent.CREATED).getFirst();
    // update retries to resolve incident
    zeebeClient.newUpdateRetriesCommand(incident.getValue().getJobKey()).retries(3).send().join();
    zeebeClient.newResolveIncidentCommand(incident.getKey()).send().join();

    // wait until workflow instance is completed
    TestUtil.waitUntil(
        () ->
            workflowInstanceRecords(WorkflowInstanceIntent.ELEMENT_COMPLETED)
                .filter(r -> r.getKey() == r.getValue().getWorkflowInstanceKey())
                .exists());

    // otherwise polling keeps creating new records
    worker.close();
    TestUtil.waitUntil(worker::isClosed);

    // assert all records which where recorded during the tests where exported
    assertRecordsExported();
  }

  private void assertRecordsExported() throws InterruptedException {
    final RecordSerializer serializer = new RecordSerializer();
    final StringDeserializer deserializer = new StringDeserializer();
    final Map<String, String> records = new HashMap<>();

    for (final ConsumerRecord<String, String> record : consumeAll()) {
      assertThat(records.containsKey(record.key())).isFalse();
      records.put(record.key(), record.value());
    }

    serializer.configure(null, true);
    RecordingExporter.getRecords()
        .forEach(
            record -> {
              final String key =
                  deserializer.deserialize(TOPIC, serializer.serialize(TOPIC, record));
              final String json = record.toJson();

              assertThat(records).contains(entry(key, json));
            });
  }

  private List<ConsumerRecord<String, String>> consumeAll() {
    final List<ConsumerRecord<String, String>> records = new ArrayList<>();
    final Duration timeout = Duration.ofSeconds(5);
    kafkaConsumer.poll(timeout).forEach(records::add);

    return records;
  }

  private KafkaConsumer<String, String> createConsumer() {
    final Properties properties = new Properties();
    properties.put("group.id", this.getClass().getName());
    properties.put("enable.auto.commit", "true");
    properties.put("auto.commit.interval.ms", "100");
    properties.put("max.poll.records", String.valueOf(Integer.MAX_VALUE));

    final KafkaConsumer<String, String> consumer =
        kafkaRule.helper().createStringConsumer(properties);
    consumer.subscribe(Collections.singletonList(TOPIC));

    return consumer;
  }
}
