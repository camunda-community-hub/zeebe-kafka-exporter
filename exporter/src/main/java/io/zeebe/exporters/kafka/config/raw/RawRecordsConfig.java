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
public final class RawRecordsConfig {

  /**
   * If a record value type is omitted in your configuration file, it will fall back to whatever is
   * configured in the defaults.
   */
  public RawRecordConfig defaults;

  /**
   * For records with a value of type {@link io.camunda.zeebe.protocol.record.ValueType#DEPLOYMENT}
   */
  public RawRecordConfig deployment;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#DEPLOYMENT_DISTRIBUTION}
   */
  public RawRecordConfig deploymentDistribution;

  /** For records with a value of type {@link io.camunda.zeebe.protocol.record.ValueType#ERROR} */
  public RawRecordConfig error;

  /**
   * For records with a value of type {@link io.camunda.zeebe.protocol.record.ValueType#INCIDENT}
   */
  public RawRecordConfig incident;

  /**
   * For records with a value of type {@link io.camunda.zeebe.protocol.record.ValueType#JOB_BATCH}
   */
  public RawRecordConfig jobBatch;

  /** For records with a value of type {@link io.camunda.zeebe.protocol.record.ValueType#JOB} */
  public RawRecordConfig job;

  /** For records with a value of type {@link io.camunda.zeebe.protocol.record.ValueType#MESSAGE} */
  public RawRecordConfig message;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#MESSAGE_SUBSCRIPTION}
   */
  public RawRecordConfig messageSubscription;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#MESSAGE_START_EVENT_SUBSCRIPTION}
   */
  public RawRecordConfig messageStartEventSubscription;

  /** For records with a value of type {@link io.camunda.zeebe.protocol.record.ValueType#PROCESS} */
  public RawRecordConfig process;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#PROCESS_EVENT}
   */
  public RawRecordConfig processEvent;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#PROCESS_INSTANCE}
   */
  public RawRecordConfig processInstance;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#PROCESS_INSTANCE_CREATION}
   */
  public RawRecordConfig processInstanceCreation;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#PROCESS_INSTANCE_RESULT}
   */
  public RawRecordConfig processInstanceResult;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#PROCESS_MESSAGE_SUBSCRIPTION}
   */
  public RawRecordConfig processMessageSubscription;

  /** For records with a value of type {@link io.camunda.zeebe.protocol.record.ValueType#TIMER} */
  public RawRecordConfig timer;

  /**
   * For records with a value of type {@link io.camunda.zeebe.protocol.record.ValueType#VARIABLE}
   */
  public RawRecordConfig variable;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#VARIABLE_DOCUMENT}
   */
  public RawRecordConfig variableDocument;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#DECISION}
   */
  public RawRecordConfig decision;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#DECISION_REQUIREMENTS}
   */
  public RawRecordConfig decisionRequirements;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#DECISION_EVALUATION}
   */
  public RawRecordConfig decisionEvaluation;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#PROCESS_INSTANCE_MODIFICATION}
   */
  public RawRecordConfig processInstanceModification;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#ESCALATION}
   */
  public RawRecordConfig escalation;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#SIGNAL_SUBSCRIPTION}
   */
  public RawRecordConfig signalSubscription;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#SIGNAL}
   */
  public RawRecordConfig signal;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#RESOURCE_DELETION}
   */
  public RawRecordConfig resourceDeletion;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#COMMAND_DISTRIBUTION}
   */
  public RawRecordConfig commandDistribution;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#PROCESS_INSTANCE_BATCH}
   */
  public RawRecordConfig processInstanceBatch;

  /**
   * For records with a value of type {@link
   * io.camunda.zeebe.protocol.record.ValueType#CHECKPOINT}
   */
  public RawRecordConfig checkpoint;
}
