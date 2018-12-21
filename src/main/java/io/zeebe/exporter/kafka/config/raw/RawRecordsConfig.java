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
package io.zeebe.exporter.kafka.config.raw;

import io.zeebe.exporter.kafka.config.RecordsConfig;
import io.zeebe.protocol.clientapi.ValueType;

public class RawRecordsConfig {
  public RawRecordConfig defaults;
  public RawRecordConfig deployment;
  public RawRecordConfig incident;
  public RawRecordConfig jobBatch;
  public RawRecordConfig job;
  public RawRecordConfig message;
  public RawRecordConfig messageSubscription;
  public RawRecordConfig raft;
  public RawRecordConfig timer;
  public RawRecordConfig workflowInstance;
  public RawRecordConfig workflowInstanceSubscription;

  public RecordsConfig parse() {
    final RecordsConfig parsed = new RecordsConfig();

    if (defaults != null) {
      parsed.defaults = defaults.parse(null);
    }

    parseOrDefault(parsed, ValueType.DEPLOYMENT, deployment);
    parseOrDefault(parsed, ValueType.INCIDENT, incident);
    parseOrDefault(parsed, ValueType.JOB, job);
    parseOrDefault(parsed, ValueType.JOB_BATCH, jobBatch);
    parseOrDefault(parsed, ValueType.MESSAGE, message);
    parseOrDefault(parsed, ValueType.MESSAGE_SUBSCRIPTION, messageSubscription);
    parseOrDefault(parsed, ValueType.RAFT, raft);
    parseOrDefault(parsed, ValueType.TIMER, timer);
    parseOrDefault(parsed, ValueType.WORKFLOW_INSTANCE, workflowInstance);
    parseOrDefault(parsed, ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION, workflowInstanceSubscription);

    return parsed;
  }

  public void parseOrDefault(RecordsConfig parsed, ValueType type, RawRecordConfig raw) {
    if (raw != null) {
      parsed.recordConfigs.put(type, raw.parse(parsed.defaults));
    }
  }
}
