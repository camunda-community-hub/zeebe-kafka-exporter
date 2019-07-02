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
package io.zeebe.exporters.kafka.serde.generic;

import com.google.protobuf.Message;
import io.zeebe.exporter.proto.Schema;
import java.util.Arrays;

public enum SchemaType {
  DEPLOYMENT(Schema.DeploymentRecord.getDefaultInstance()),
  ERROR(Schema.ErrorRecord.getDefaultInstance()),
  INCIDENT(Schema.IncidentRecord.getDefaultInstance()),
  JOB(Schema.JobRecord.getDefaultInstance()),
  JOB_BATCH(Schema.JobBatchRecord.getDefaultInstance()),
  MESSAGE(Schema.MessageRecord.getDefaultInstance()),
  MESSAGE_START_EVENT(Schema.MessageStartEventSubscriptionRecord.getDefaultInstance()),
  MESSAGE_SUBSCRIPTION(Schema.MessageSubscriptionRecord.getDefaultInstance()),
  RECORD_ID(Schema.RecordId.getDefaultInstance()),
  RECORD_METADATA(Schema.RecordMetadata.getDefaultInstance()),
  TIMER(Schema.TimerRecord.getDefaultInstance()),
  VARIABLE(Schema.VariableRecord.getDefaultInstance()),
  VARIABLE_DOCUMENT(Schema.VariableDocumentRecord.getDefaultInstance()),
  WORKFLOW_INSTANCE(Schema.WorkflowInstanceRecord.getDefaultInstance()),
  WORKFLOW_INSTANCE_CREATION(Schema.WorkflowInstanceCreationRecord.getDefaultInstance()),
  WORKFLOW_INSTANCE_SUBSCRIPTION(Schema.WorkflowInstanceSubscriptionRecord.getDefaultInstance());

  private final String typeName;
  private final Message instance;

  SchemaType(Message instance) {
    this.typeName = instance.getDescriptorForType().getName();
    this.instance = instance;
  }

  public String getTypeName() {
    return typeName;
  }

  public Message getInstance() {
    return instance;
  }

  public static Message forType(String typeName) {
    final SchemaType[] values = values();
    for (final SchemaType value : values) {
      if (value.typeName.equals(typeName)) {
        return value.instance;
      }
    }

    throw new IllegalArgumentException(
        String.format("Expected one of %s, but got %s", Arrays.toString(values), typeName));
  }
}
