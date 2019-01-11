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

import com.google.protobuf.*;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.RejectionType;
import io.zeebe.protocol.clientapi.ValueType;
import io.zeebe.protocol.impl.record.value.deployment.ResourceType;
import io.zeebe.protocol.impl.record.value.incident.ErrorType;
import io.zeebe.protocol.intent.*;
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
        raft().build(),
        timer().build(),
        variable().build(),
        workflowInstance().build(),
        workflowInstanceSubscription().build());
  }

  public Schema.RecordMetadata.Builder metadata() {
    return Schema.RecordMetadata.newBuilder()
        .setKey(1L)
        .setPosition(1L)
        .setPartitionId(3)
        .setRaftTerm(1)
        .setProducerId(1)
        .setSourceRecordPosition(1L)
        .setTimestamp(timestamp())
        .setRecordType(RecordType.COMMAND_REJECTION.name())
        .setValueType(ValueType.JOB.name())
        .setIntent(JobIntent.ACTIVATED.name())
        .setRejectionReason("Invalid things")
        .setRejectionType(RejectionType.INVALID_ARGUMENT.name());
  }

  public Schema.DeploymentRecord.Resource.Builder deploymentResource() {
    return Schema.DeploymentRecord.Resource.newBuilder()
        .setResource(ByteString.copyFromUtf8("I am a deployment resource"))
        .setResourceName("resource.bpmn")
        .setResourceType(ResourceType.BPMN_XML.name());
  }

  public Schema.DeploymentRecord.Workflow.Builder deploymentWorkflow() {
    return Schema.DeploymentRecord.Workflow.newBuilder()
        .setBpmnProcessId("process")
        .setResourceName("resource.bpmn")
        .setVersion(1)
        .setWorkflowKey(1L);
  }

  public Schema.DeploymentRecord.Builder deployment() {
    return Schema.DeploymentRecord.newBuilder()
        .setMetadata(
            metadata()
                .setValueType(ValueType.DEPLOYMENT.name())
                .setIntent(DeploymentIntent.CREATE.name()))
        .addResources(deploymentResource())
        .addWorkflows(deploymentWorkflow());
  }

  public Schema.IncidentRecord.Builder incident() {
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

  public Schema.JobRecord.Builder job() {
    return Schema.JobRecord.newBuilder()
        .setDeadline(timestamp())
        .setErrorMessage("error")
        .setRetries(1)
        .setType("type")
        .setWorker("worker")
        .setCustomHeaders(struct())
        .setPayload(struct())
        .setMetadata(
            metadata().setValueType(ValueType.JOB.name()).setIntent(JobIntent.ACTIVATED.name()));
  }

  public Schema.JobBatchRecord.Builder jobBatch() {
    final Schema.JobRecord.Builder job = job();
    return Schema.JobBatchRecord.newBuilder()
        .setAmount(1)
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
        .setPayload(struct())
        .setMetadata(
            metadata()
                .setIntent(MessageIntent.PUBLISH.name())
                .setValueType(ValueType.MESSAGE.name()));
  }

  public Schema.MessageSubscriptionRecord.Builder messageSubscription() {
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

  public Schema.MessageStartEventSubscriptionRecord.Builder messageStartEventSubscription() {
    return Schema.MessageStartEventSubscriptionRecord.newBuilder()
        .setStartEventId("start")
        .setMessageName("name")
        .setWorkflowKey(1L)
        .setMetadata(
            metadata()
                .setIntent(MessageStartEventSubscriptionIntent.OPEN.name())
                .setValueType(ValueType.MESSAGE_START_EVENT_SUBSCRIPTION.name()));
  }

  public Schema.RaftRecord.Member.Builder raftMember() {
    return Schema.RaftRecord.Member.newBuilder().setNodeId(1);
  }

  public Schema.RaftRecord.Builder raft() {
    return Schema.RaftRecord.newBuilder().addMembers(raftMember());
  }

  public Schema.RecordId.Builder recordId() {
    return Schema.RecordId.newBuilder().setPartitionId(3).setPosition(1L);
  }

  public Schema.TimerRecord.Builder timer() {
    return Schema.TimerRecord.newBuilder()
        .setDueDate(1000L)
        .setElementInstanceKey(1L)
        .setHandlerFlowNodeId("element")
        .setMetadata(
            metadata().setValueType(ValueType.TIMER.name()).setIntent(TimerIntent.CREATE.name()));
  }

  public Schema.VariableRecord.Builder variable() {
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

  public Schema.WorkflowInstanceRecord.Builder workflowInstance() {
    return Schema.WorkflowInstanceRecord.newBuilder()
        .setFlowScopeKey(1L)
        .setBpmnProcessId("process")
        .setElementId("element")
        .setVersion(1)
        .setWorkflowInstanceKey(1L)
        .setWorkflowKey(1L)
        .setPayload(struct())
        .setMetadata(
            metadata()
                .setIntent(WorkflowInstanceIntent.ELEMENT_ACTIVATING.name())
                .setValueType(ValueType.WORKFLOW_INSTANCE.name()));
  }

  public Schema.WorkflowInstanceSubscriptionRecord.Builder workflowInstanceSubscription() {
    return Schema.WorkflowInstanceSubscriptionRecord.newBuilder()
        .setElementInstanceKey(1L)
        .setMessageName("name")
        .setWorkflowInstanceKey(1L)
        .setPayload(struct())
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
