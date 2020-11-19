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
package io.zeebe.exporters.kafka.tck;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.client.api.worker.JobHandler;
import io.zeebe.client.api.worker.JobWorker;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.intent.IncidentIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import io.zeebe.protocol.record.value.IncidentRecordValue;
import io.zeebe.protocol.record.value.WorkflowInstanceRecordValue;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;

/**
 * An experimental exporter TCK. The goal of the TCK would be to provide a simple baseline for
 * exporter authors to check the validity of their exporters, depending on certain properties of
 * said exporters (e.g. maintains order, exports all, etc.)
 *
 * <p>This is currently very much a work in a progress, so use at your own risk.
 */
public final class ExporterTechnologyCompatibilityKit {

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
  private final RecordStreamSupplier recordSupplier;

  /**
   * @param client a configured ZeebeClient which can be used to deploy workflows, publish messages,
   *     etc.
   * @param recordSupplier a supplier which can return a stream to the exported records (at the time
   *     of the call, not necessarily all exported records ever); the stream may be slightly delayed
   */
  public ExporterTechnologyCompatibilityKit(
      final @NonNull ZeebeClient client, final @NonNull RecordStreamSupplier recordSupplier) {
    this.client = Objects.requireNonNull(client);
    this.recordSupplier = Objects.requireNonNull(recordSupplier);
  }

  /** Runs a sample workload on the broker, exporting several records of different types. */
  public void performSampleWorkload() {
    deployWorkflow();

    final Map<String, Object> variables = new HashMap<>();
    variables.put("orderId", CORRELATION_KEY);
    variables.put("largeValue", "x".repeat(8192));
    variables.put("unicode", "Á");

    final long workflowInstanceKey = createWorkflowInstance(variables);
    final AtomicBoolean fail = new AtomicBoolean(true);
    final JobWorker worker = createJobWorker((jobClient, job) -> handleJob(fail, jobClient, job));

    publishMessage();

    final Record<IncidentRecordValue> incident = awaitIncidentRaised(workflowInstanceKey);
    client.newUpdateRetriesCommand(incident.getValue().getJobKey()).retries(3).send().join();
    client.newResolveIncidentCommand(incident.getKey()).send().join();

    // wrap up
    awaitWorkflowCompletion(workflowInstanceKey);
    worker.close();
  }

  /**
   * Asserts that the records given contain exactly the same records as those provided by the {@code
   * recordSupplier} the TCK was constructed with. This allows to compare a trusted and valid
   * exporter with the exporter under test.
   *
   * @param actualRecords the records exported by the exporter under test
   * @throws AssertionError if {@code actualRecords} contain more or less records than the {@code
   *     recordSupplier}
   * @throws AssertionError if {@code actualRecords} contains a record which is not in the stream
   *     provided by the {@code recordSupplier}
   * @throws AssertionError if {@code actualRecords} is missing a record which is in the stream
   *     provided by the {@code recordSupplier}
   */
  public void assertAllRecordsExported(final @Nullable List<Record<?>> actualRecords) {
    final List<Record<?>> expectedRecords = recordSupplier.get().collect(Collectors.toList());

    Assertions.assertThat(expectedRecords).isNotEmpty();
    Assertions.assertThat(actualRecords)
        .hasSameSizeAs(expectedRecords)
        .containsExactlyInAnyOrderElementsOf(expectedRecords);
  }

  /**
   * Asserts that the records exported by the exporter under test are logically ordered relative to
   * their partition ID. More specifically, that for every record of partition X, each subsequent
   * record has a greater position than the last, regardless if there are records of partition Y
   * with lower position in between.
   *
   * <p>NOTE: this assertion only makes sense for exporters which preserve ordering
   *
   * @param actualRecords all records exported by the exporter under test, in the order in which
   *     they were exported
   * @throws AssertionError if {@code actualRecords} is null or empty
   * @throws AssertionError if {@code actualRecords} contains two records (consecutive or not) with
   *     the same partition ID, and the first exported record has a position greater than or equal
   *     to that of the second
   */
  public void assertRecordsMaintainOrderPerPartition(
      final @Nullable List<Record<?>> actualRecords) {
    Assertions.assertThat(actualRecords).isNotEmpty();

    final Map<Integer, List<Record<?>>> actualRecordsPerPartition =
        Objects.requireNonNull(actualRecords).stream()
            .collect(Collectors.groupingBy(Record::getPartitionId));
    Assertions.assertThat(actualRecordsPerPartition)
        .allSatisfy(
            (partitionId, records) ->
                Assertions.assertThat(records)
                    .isSortedAccordingTo(Comparator.comparing(Record::getPosition)));
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
    return recordSupplier
        .get()
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
        .untilAsserted(
            () -> Assertions.assertThat(getProcessCompleted(workflowInstanceKey)).isPresent());
  }

  @SuppressWarnings({"unchecked", "java:S1905"})
  private Optional<Record<WorkflowInstanceRecordValue>> getProcessCompleted(
      final long workflowInstanceKey) {
    return recordSupplier
        .get()
        .filter(r -> r.getIntent() == WorkflowInstanceIntent.ELEMENT_COMPLETED)
        .filter(r -> r.getKey() == workflowInstanceKey)
        .map(r -> (Record<WorkflowInstanceRecordValue>) r)
        .filter(r -> r.getValue().getBpmnElementType() == BpmnElementType.PROCESS)
        .findFirst();
  }
}
