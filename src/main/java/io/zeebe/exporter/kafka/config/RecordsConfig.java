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
package io.zeebe.exporter.kafka.config;

import io.zeebe.protocol.clientapi.ValueType;
import java.util.EnumMap;

public class RecordsConfig {
  public RecordConfig defaults = new RecordConfig();
  public final EnumMap<ValueType, RecordConfig> recordConfigs;

  public RecordsConfig() {
    recordConfigs = new EnumMap<>(ValueType.class);

    recordConfigs.put(ValueType.DEPLOYMENT, new RecordConfig());
    recordConfigs.put(ValueType.INCIDENT, new RecordConfig());
    recordConfigs.put(ValueType.JOB_BATCH, new RecordConfig());
    recordConfigs.put(ValueType.JOB, new RecordConfig());
    recordConfigs.put(ValueType.MESSAGE, new RecordConfig());
    recordConfigs.put(ValueType.MESSAGE_SUBSCRIPTION, new RecordConfig());
    recordConfigs.put(ValueType.RAFT, new RecordConfig());
    recordConfigs.put(ValueType.TIMER, new RecordConfig());
    recordConfigs.put(ValueType.WORKFLOW_INSTANCE, new RecordConfig());
    recordConfigs.put(ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION, new RecordConfig());
  }

  public RecordConfig forType(ValueType type) {
    return recordConfigs.get(type);
  }
}
