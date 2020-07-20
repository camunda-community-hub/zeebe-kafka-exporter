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
package io.zeebe.exporters.kafka.config.raw;

@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class RawRecordsConfig {

  /**
   * If a record value type is omitted in your configuration file, it will fall back to whatever is
   * configured in the defaults.
   */
  public RawRecordConfig defaults;

  /** For records with a value of type {@link io.zeebe.protocol.record.ValueType#DEPLOYMENT} */
  public RawRecordConfig deployment;

  /** For records with a value of type {@link io.zeebe.protocol.record.ValueType#ERROR} */
  public RawRecordConfig error;

  /** For records with a value of type {@link io.zeebe.protocol.record.ValueType#INCIDENT} */
  public RawRecordConfig incident;

  /** For records with a value of type {@link io.zeebe.protocol.record.ValueType#JOB_BATCH} */
  public RawRecordConfig jobBatch;

  /** For records with a value of type {@link io.zeebe.protocol.record.ValueType#JOB} */
  public RawRecordConfig job;

  /** For records with a value of type {@link io.zeebe.protocol.record.ValueType#MESSAGE} */
  public RawRecordConfig message;

  /**
   * For records with a value of type {@link
   * io.zeebe.protocol.record.ValueType#MESSAGE_SUBSCRIPTION}
   */
  public RawRecordConfig messageSubscription;

  /**
   * For records with a value of type {@link
   * io.zeebe.protocol.record.ValueType#MESSAGE_START_EVENT_SUBSCRIPTION}
   */
  public RawRecordConfig messageStartEventSubscription;

  /** For records with a value of type {@link io.zeebe.protocol.record.ValueType#TIMER} */
  public RawRecordConfig timer;

  /** For records with a value of type {@link io.zeebe.protocol.record.ValueType#VARIABLE} */
  public RawRecordConfig variable;

  /**
   * For records with a value of type {@link io.zeebe.protocol.record.ValueType#VARIABLE_DOCUMENT}
   */
  public RawRecordConfig variableDocument;

  /**
   * For records with a value of type {@link io.zeebe.protocol.record.ValueType#WORKFLOW_INSTANCE}
   */
  public RawRecordConfig workflowInstance;

  /**
   * For records with a value of type {@link
   * io.zeebe.protocol.record.ValueType#WORKFLOW_INSTANCE_CREATION}
   */
  public RawRecordConfig workflowInstanceCreation;

  /**
   * For records with a value of type {@link
   * io.zeebe.protocol.record.ValueType#WORKFLOW_INSTANCE_RESULT}
   */
  public RawRecordConfig workflowInstanceResult;

  /**
   * For records with a value of type {@link
   * io.zeebe.protocol.record.ValueType#WORKFLOW_INSTANCE_SUBSCRIPTION}
   */
  public RawRecordConfig workflowInstanceSubscription;
}
