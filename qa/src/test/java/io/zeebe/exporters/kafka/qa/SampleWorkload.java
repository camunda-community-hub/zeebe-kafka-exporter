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

import static org.assertj.core.api.Assertions.assertThat;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.client.api.worker.JobHandler;
import io.zeebe.client.api.worker.JobWorker;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.RecordAssert;
import io.zeebe.protocol.record.intent.IncidentIntent;
import io.zeebe.protocol.record.intent.MessageIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import io.zeebe.protocol.record.value.IncidentRecordValue;
import io.zeebe.protocol.record.value.WorkflowInstanceRecordValue;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;

public final class SampleWorkload {
  private static final String JOB_TYPE = "work";
  private static final String MESSAGE_NAME = "catch";
  private static final String CORRELATION_KEY = "foo-bar-123";
  private static final String PROCESS_NAME = "testProcess";
  private static final String PROCESS_FILE_NAME = "sample_workflow.bpmn";
  private static final String TASK_NAME = "task";
  private static final BpmnModelInstance SAMPLE_WORKFLOW =
      Bpmn.createExecutableProcess(PROCESS_NAME)
          .startEvent()
          .intermediateCatchEvent(
              "message",
              e -> e.message(m -> m.name(MESSAGE_NAME).zeebeCorrelationKeyExpression("orderId")))
          .serviceTask(TASK_NAME, t -> t.zeebeJobType(JOB_TYPE).zeebeTaskHeader("foo", "bar"))
          .endEvent()
          .done();

  private final ZeebeClient client;
  private final DebugHttpExporterClient exporterClient;

  private long endMarkerKey;

  public SampleWorkload(final ZeebeClient client, final DebugHttpExporterClient exporterClient) {
    this.client = Objects.requireNonNull(client);
    this.exporterClient = Objects.requireNonNull(exporterClient);
  }

  public void execute() {
    execute(() -> {});
  }

  /** Runs a sample workload on the broker, exporting several records of different types. */
  public void execute(final Runnable midpointHook) {
    deployWorkflow();

    final Map<String, Object> variables = new HashMap<>();
    variables.put("orderId", CORRELATION_KEY);
    variables.put("largeValue", "x".repeat(8192));
    variables.put("unicode", "Á");

    final long workflowInstanceKey = createWorkflowInstance(variables);
    final AtomicBoolean fail = new AtomicBoolean(true);
    final JobWorker worker = createJobWorker((jobClient, job) -> handleJob(fail, jobClient, job));

    midpointHook.run();
    publishMessage();

    final Record<IncidentRecordValue> incident = awaitIncidentRaised(workflowInstanceKey);
    client.newUpdateRetriesCommand(incident.getValue().getJobKey()).retries(3).send().join();
    client.newResolveIncidentCommand(incident.getKey()).send().join();

    // wrap up
    awaitWorkflowCompletion(workflowInstanceKey);
    worker.close();
    publishEndMarker();
  }

  public List<Record<?>> getExpectedRecords(final Duration timeout) {
    final var records = new ArrayList<Record<?>>();
    assertThat(endMarkerKey).as("the end marker was published so it can be looked up").isPositive();

    Awaitility.await("until all expected records have been exported")
        .atMost(timeout)
        .pollInterval(Duration.ofMillis(250))
        .pollDelay(Duration.ZERO)
        .pollInSameThread()
        .untilAsserted(
            () -> {
              records.clear();
              records.addAll(exporterClient.streamRecords().collect(Collectors.toList()));
              assertEndMarkerExported(records);
            });

    return records;
  }

  private void assertEndMarkerExported(final ArrayList<Record<?>> records) {
    assertThat(records)
        .last()
        .as("exported records contain the last expected record")
        .satisfies(
            r -> RecordAssert.assertThat(r).hasKey(endMarkerKey).hasIntent(MessageIntent.DELETED));
  }

  private void publishEndMarker() {
    final var response =
        client
            .newPublishMessageCommand()
            .messageName("endMarker")
            .correlationKey("endMarker")
            .messageId("endMarker")
            .timeToLive(Duration.ZERO)
            .send()
            .join();

    endMarkerKey = response.getMessageKey();
  }

  @NonNull
  private Record<IncidentRecordValue> awaitIncidentRaised(final long workflowInstanceKey) {
    return Awaitility.await("await incident to be raised")
        .pollInterval(Duration.ofMillis(200))
        .atMost(Duration.ofSeconds(10))
        .until(() -> findIncident(workflowInstanceKey), Optional::isPresent)
        .orElseThrow();
  }

  @SuppressWarnings({"unchecked", "java:S1905"})
  @NonNull
  private Optional<Record<IncidentRecordValue>> findIncident(final long workflowInstanceKey) {
    return exporterClient
        .streamRecords()
        .filter(r -> r.getIntent() == IncidentIntent.CREATED)
        .map(r -> (Record<IncidentRecordValue>) r)
        .filter(r -> r.getValue().getWorkflowInstanceKey() == workflowInstanceKey)
        .filter(r -> r.getValue().getElementId().equals(TASK_NAME))
        .findFirst();
  }

  private void handleJob(
      final @NonNull AtomicBoolean fail,
      final @NonNull JobClient jobClient,
      final @NonNull ActivatedJob job) {
    if (fail.getAndSet(false)) {
      jobClient.newFailCommand(job.getKey()).retries(0).errorMessage("failed").send().join();
    } else {
      jobClient.newCompleteCommand(job.getKey()).send().join();
    }
  }

  private void deployWorkflow() {
    client.newDeployCommand().addWorkflowModel(SAMPLE_WORKFLOW, PROCESS_FILE_NAME).send().join();
  }

  private long createWorkflowInstance(final @NonNull Map<String, Object> variables) {
    return client
        .newCreateInstanceCommand()
        .bpmnProcessId(PROCESS_NAME)
        .latestVersion()
        .variables(variables)
        .send()
        .join()
        .getWorkflowInstanceKey();
  }

  private JobWorker createJobWorker(final @NonNull JobHandler handler) {
    return client.newWorker().jobType(JOB_TYPE).handler(handler).open();
  }

  private void publishMessage() {
    client
        .newPublishMessageCommand()
        .messageName(MESSAGE_NAME)
        .correlationKey(CORRELATION_KEY)
        .send()
        .join();
  }

  private void awaitWorkflowCompletion(final long workflowInstanceKey) {
    Awaitility.await("await workflow " + workflowInstanceKey + " completion")
        .pollInterval(Duration.ofMillis(200))
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(getProcessCompleted(workflowInstanceKey)).isPresent());
  }

  @SuppressWarnings({"unchecked", "java:S1905"})
  private Optional<Record<WorkflowInstanceRecordValue>> getProcessCompleted(
      final long workflowInstanceKey) {
    return exporterClient
        .streamRecords()
        .filter(r -> r.getIntent() == WorkflowInstanceIntent.ELEMENT_COMPLETED)
        .filter(r -> r.getKey() == workflowInstanceKey)
        .map(r -> (Record<WorkflowInstanceRecordValue>) r)
        .filter(r -> r.getValue().getBpmnElementType() == BpmnElementType.PROCESS)
        .findFirst();
  }
}
