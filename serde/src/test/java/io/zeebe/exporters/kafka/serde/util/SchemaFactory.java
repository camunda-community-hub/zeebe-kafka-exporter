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
package io.zeebe.exporters.kafka.serde.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import io.zeebe.exporter.api.record.value.deployment.ResourceType;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.protocol.ErrorType;
import io.zeebe.protocol.RecordType;
import io.zeebe.protocol.RejectionType;
import io.zeebe.protocol.ValueType;
import io.zeebe.protocol.VariableDocumentUpdateSemantic;
import io.zeebe.protocol.intent.DeploymentIntent;
import io.zeebe.protocol.intent.IncidentIntent;
import io.zeebe.protocol.intent.JobBatchIntent;
import io.zeebe.protocol.intent.JobIntent;
import io.zeebe.protocol.intent.MessageIntent;
import io.zeebe.protocol.intent.MessageStartEventSubscriptionIntent;
import io.zeebe.protocol.intent.MessageSubscriptionIntent;
import io.zeebe.protocol.intent.TimerIntent;
import io.zeebe.protocol.intent.VariableDocumentIntent;
import io.zeebe.protocol.intent.VariableIntent;
import io.zeebe.protocol.intent.WorkflowInstanceCreationIntent;
import io.zeebe.protocol.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.intent.WorkflowInstanceSubscriptionIntent;
import java.util.Arrays;
import java.util.List;

/** Test utility providing pre-filled out records */
public class SchemaFactory {
  public List<Message> records() {
    return Arrays.asList(
        deployment().build(),
        incident().build(),
        job().build(),
        jobBatch().build(),
        message().build(),
        messageSubscription().build(),
        messageStartEventSubscription().build(),
        timer().build(),
        variable().build(),
        variableDocument().build(),
        workflowInstance().build(),
        workflowInstanceSubscription().build());
  }

  private Schema.RecordMetadata.Builder metadata() {
    return Schema.RecordMetadata.newBuilder()
        .setKey(1L)
        .setPosition(1L)
        .setPartitionId(3)
        .setProducerId(1)
        .setSourceRecordPosition(1L)
        .setTimestamp(timestamp())
        .setRecordType(RecordType.COMMAND_REJECTION.name())
        .setValueType(ValueType.JOB.name())
        .setIntent(JobIntent.ACTIVATED.name())
        .setRejectionReason("Invalid things")
        .setRejectionType(RejectionType.INVALID_ARGUMENT.name());
  }

  private Schema.DeploymentRecord.Resource.Builder deploymentResource() {
    return Schema.DeploymentRecord.Resource.newBuilder()
        .setResource(ByteString.copyFromUtf8("I am a deployment resource"))
        .setResourceName("resource.bpmn")
        .setResourceType(ResourceType.BPMN_XML.name());
  }

  private Schema.DeploymentRecord.Workflow.Builder deploymentWorkflow() {
    return Schema.DeploymentRecord.Workflow.newBuilder()
        .setBpmnProcessId("process")
        .setResourceName("resource.bpmn")
        .setVersion(1)
        .setWorkflowKey(1L);
  }

  private Schema.DeploymentRecord.Builder deployment() {
    return Schema.DeploymentRecord.newBuilder()
        .setMetadata(
            metadata()
                .setValueType(ValueType.DEPLOYMENT.name())
                .setIntent(DeploymentIntent.CREATE.name()))
        .addResources(deploymentResource())
        .addWorkflows(deploymentWorkflow());
  }

  private Schema.IncidentRecord.Builder incident() {
    return Schema.IncidentRecord.newBuilder()
        .setBpmnProcessId("process")
        .setElementId("element")
        .setErrorMessage("error")
        .setErrorType(ErrorType.CONDITION_ERROR.name())
        .setElementInstanceKey(1L)
        .setJobKey(3L)
        .setMetadata(
            metadata()
                .setIntent(IncidentIntent.CREATE.name())
                .setValueType(ValueType.INCIDENT.name()));
  }

  private Schema.JobRecord.Builder job() {
    return Schema.JobRecord.newBuilder()
        .setDeadline(timestamp())
        .setErrorMessage("error")
        .setRetries(1)
        .setType("type")
        .setWorker("worker")
        .setCustomHeaders(struct())
        .setVariables(struct())
        .setMetadata(
            metadata().setValueType(ValueType.JOB.name()).setIntent(JobIntent.ACTIVATED.name()));
  }

  private Schema.JobBatchRecord.Builder jobBatch() {
    final Schema.JobRecord.Builder job = job();
    return Schema.JobBatchRecord.newBuilder()
        .setMaxJobsToActivate(1)
        .setTimeout(1000L)
        .setType(job.getType())
        .setWorker(job.getWorker())
        .addJobs(job)
        .addJobKeys(1L)
        .setMetadata(
            metadata()
                .setValueType(ValueType.JOB_BATCH.name())
                .setIntent(JobBatchIntent.ACTIVATE.name()));
  }

  public Schema.MessageRecord.Builder message() {
    return Schema.MessageRecord.newBuilder()
        .setCorrelationKey("1")
        .setMessageId("id")
        .setName("name")
        .setTimeToLive(1000L)
        .setVariables(struct())
        .setMetadata(
            metadata()
                .setIntent(MessageIntent.PUBLISH.name())
                .setValueType(ValueType.MESSAGE.name()));
  }

  private Schema.MessageSubscriptionRecord.Builder messageSubscription() {
    return Schema.MessageSubscriptionRecord.newBuilder()
        .setCorrelationKey("1")
        .setElementInstanceKey(1L)
        .setMessageName("name")
        .setWorkflowInstanceKey(1L)
        .setMetadata(
            metadata()
                .setValueType(ValueType.MESSAGE_SUBSCRIPTION.name())
                .setIntent(MessageSubscriptionIntent.CLOSE.name()));
  }

  private Schema.MessageStartEventSubscriptionRecord.Builder messageStartEventSubscription() {
    return Schema.MessageStartEventSubscriptionRecord.newBuilder()
        .setStartEventId("start")
        .setMessageName("name")
        .setWorkflowKey(1L)
        .setMetadata(
            metadata()
                .setIntent(MessageStartEventSubscriptionIntent.OPEN.name())
                .setValueType(ValueType.MESSAGE_START_EVENT_SUBSCRIPTION.name()));
  }

  private Schema.TimerRecord.Builder timer() {
    return Schema.TimerRecord.newBuilder()
        .setDueDate(1000L)
        .setElementInstanceKey(1L)
        .setHandlerFlowNodeId("element")
        .setMetadata(
            metadata().setValueType(ValueType.TIMER.name()).setIntent(TimerIntent.CREATE.name()));
  }

  private Schema.VariableRecord.Builder variable() {
    return Schema.VariableRecord.newBuilder()
        .setScopeKey(1L)
        .setWorkflowInstanceKey(1L)
        .setName("var")
        .setValue("1")
        .setMetadata(
            metadata()
                .setIntent(VariableIntent.CREATED.name())
                .setValueType(ValueType.VARIABLE.name()));
  }

  private Schema.VariableDocumentRecord.Builder variableDocument() {
    return Schema.VariableDocumentRecord.newBuilder()
        .setScopeKey(1L)
        .setDocument(struct())
        .setUpdateSemantics(VariableDocumentUpdateSemantic.LOCAL.name())
        .setMetadata(
            metadata()
                .setIntent(VariableDocumentIntent.UPDATED.name())
                .setValueType(ValueType.VARIABLE_DOCUMENT.name()));
  }

  public Schema.WorkflowInstanceRecord.Builder workflowInstance() {
    return Schema.WorkflowInstanceRecord.newBuilder()
        .setFlowScopeKey(1L)
        .setBpmnProcessId("process")
        .setElementId("element")
        .setVersion(1)
        .setWorkflowInstanceKey(1L)
        .setWorkflowKey(1L)
        .setMetadata(
            metadata()
                .setIntent(WorkflowInstanceIntent.ELEMENT_ACTIVATING.name())
                .setValueType(ValueType.WORKFLOW_INSTANCE.name()));
  }

  private Schema.WorkflowInstanceCreationRecord.Builder workflowInstanceCreation() {
    return Schema.WorkflowInstanceCreationRecord.newBuilder()
        .setBpmnProcessId("process")
        .setVariables(struct())
        .setVersion(1)
        .setWorkflowInstanceKey(1)
        .setWorkflowKey(1)
        .setMetadata(
            metadata()
                .setIntent(WorkflowInstanceCreationIntent.CREATED.name())
                .setValueType(ValueType.WORKFLOW_INSTANCE_CREATION.name()));
  }

  private Schema.WorkflowInstanceSubscriptionRecord.Builder workflowInstanceSubscription() {
    return Schema.WorkflowInstanceSubscriptionRecord.newBuilder()
        .setElementInstanceKey(1L)
        .setMessageName("name")
        .setWorkflowInstanceKey(1L)
        .setVariables(struct())
        .setMetadata(
            metadata()
                .setIntent(WorkflowInstanceSubscriptionIntent.CORRELATE.name())
                .setValueType(ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION.name()));
  }

  private Timestamp.Builder timestamp() {
    return Timestamp.newBuilder().setSeconds(5).setNanos(1);
  }

  private Struct.Builder struct() {
    return Struct.newBuilder().putFields("number", Value.newBuilder().setNumberValue(2.0).build());
  }
}
