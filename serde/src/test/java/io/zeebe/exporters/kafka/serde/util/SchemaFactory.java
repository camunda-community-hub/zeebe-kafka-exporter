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
import com.google.protobuf.Value;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.exporter.proto.Schema.VariableDocumentRecord.UpdateSemantics;
import io.zeebe.protocol.record.RejectionType;
import io.zeebe.protocol.record.intent.DeploymentIntent;
import io.zeebe.protocol.record.intent.IncidentIntent;
import io.zeebe.protocol.record.intent.JobBatchIntent;
import io.zeebe.protocol.record.intent.JobIntent;
import io.zeebe.protocol.record.intent.MessageIntent;
import io.zeebe.protocol.record.intent.MessageStartEventSubscriptionIntent;
import io.zeebe.protocol.record.intent.MessageSubscriptionIntent;
import io.zeebe.protocol.record.intent.TimerIntent;
import io.zeebe.protocol.record.intent.VariableDocumentIntent;
import io.zeebe.protocol.record.intent.VariableIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceCreationIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.intent.WorkflowInstanceSubscriptionIntent;
import io.zeebe.protocol.record.value.ErrorType;
import io.zeebe.protocol.record.value.deployment.ResourceType;
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
        .setSourceRecordPosition(1L)
        .setTimestamp(System.currentTimeMillis())
        .setRecordType(Schema.RecordMetadata.RecordType.COMMAND_REJECTION)
        .setValueType(Schema.RecordMetadata.ValueType.JOB)
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
        .addDeployedWorkflows(deploymentWorkflow())
        .addResources(deploymentResource())
        .setMetadata(
            metadata()
                .setValueType(Schema.RecordMetadata.ValueType.DEPLOYMENT)
                .setIntent(DeploymentIntent.CREATE.name()));
  }

  private Schema.IncidentRecord.Builder incident() {
    return Schema.IncidentRecord.newBuilder()
        .setBpmnProcessId("process")
        .setElementId("element")
        .setErrorMessage("error")
        .setErrorType(ErrorType.CONDITION_ERROR.name())
        .setElementInstanceKey(1L)
        .setJobKey(3L)
        .setVariableScopeKey(1L)
        .setMetadata(
            metadata()
                .setIntent(IncidentIntent.CREATE.name())
                .setValueType(Schema.RecordMetadata.ValueType.INCIDENT));
  }

  private Schema.JobRecord.Builder job() {
    return Schema.JobRecord.newBuilder()
        .setDeadline(System.currentTimeMillis())
        .setErrorMessage("error")
        .setRetries(1)
        .setType("type")
        .setWorker("worker")
        .setCustomHeaders(struct())
        .setVariables(struct())
        .setMetadata(
            metadata()
                .setValueType(Schema.RecordMetadata.ValueType.JOB)
                .setIntent(JobIntent.ACTIVATED.name()));
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
                .setValueType(Schema.RecordMetadata.ValueType.JOB_BATCH)
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
                .setValueType(Schema.RecordMetadata.ValueType.MESSAGE));
  }

  private Schema.MessageSubscriptionRecord.Builder messageSubscription() {
    return Schema.MessageSubscriptionRecord.newBuilder()
        .setCorrelationKey("1")
        .setElementInstanceKey(1L)
        .setMessageName("name")
        .setWorkflowInstanceKey(1L)
        .setMetadata(
            metadata()
                .setValueType(Schema.RecordMetadata.ValueType.MESSAGE_SUBSCRIPTION)
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
                .setValueType(Schema.RecordMetadata.ValueType.MESSAGE_START_EVENT_SUBSCRIPTION));
  }

  private Schema.TimerRecord.Builder timer() {
    return Schema.TimerRecord.newBuilder()
        .setDueDate(1000L)
        .setElementInstanceKey(1L)
        .setTargetFlowNodeId("element")
        .setMetadata(
            metadata()
                .setValueType(Schema.RecordMetadata.ValueType.TIMER)
                .setIntent(TimerIntent.CREATE.name()));
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
                .setValueType(Schema.RecordMetadata.ValueType.VARIABLE));
  }

  private Schema.VariableDocumentRecord.Builder variableDocument() {
    return Schema.VariableDocumentRecord.newBuilder()
        .setScopeKey(1L)
        .setVariables(struct())
        .setUpdateSemantics(UpdateSemantics.LOCAL)
        .setMetadata(
            metadata()
                .setIntent(VariableDocumentIntent.UPDATED.name())
                .setValueType(Schema.RecordMetadata.ValueType.VARIABLE_DOCUMENT));
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
                .setValueType(Schema.RecordMetadata.ValueType.WORKFLOW_INSTANCE));
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
                .setValueType(Schema.RecordMetadata.ValueType.WORKFLOW_INSTANCE_CREATION));
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
                .setValueType(Schema.RecordMetadata.ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION));
  }

  private Struct.Builder struct() {
    return Struct.newBuilder().putFields("number", Value.newBuilder().setNumberValue(2.0).build());
  }
}
