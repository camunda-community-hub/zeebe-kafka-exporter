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
package io.zeebe.exporters.kafka.config.parser;

import io.zeebe.exporters.kafka.config.RecordConfig;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.exporters.kafka.config.toml.TomlRecordConfig;
import io.zeebe.exporters.kafka.config.toml.TomlRecordsConfig;
import io.zeebe.protocol.RecordType;
import io.zeebe.protocol.ValueType;
import java.util.EnumSet;

public class TomlRecordsConfigParser implements Parser<TomlRecordsConfig, RecordsConfig> {
  static final String DEFAULT_TOPIC_NAME = "zeebe";
  static final EnumSet<RecordType> DEFAULT_ALLOWED_TYPES = EnumSet.allOf(RecordType.class);

  private final Parser<TomlRecordConfig, RecordConfig> recordConfigParser;

  TomlRecordsConfigParser() {
    this.recordConfigParser = new TomlRecordConfigParser();
  }

  public TomlRecordsConfigParser(Parser<TomlRecordConfig, RecordConfig> recordConfigParser) {
    this.recordConfigParser = recordConfigParser;
  }

  @Override
  public RecordsConfig parse(TomlRecordsConfig config) {
    final RecordsConfig parsed = new RecordsConfig();

    parsed.defaults = recordConfigParser.parse(config.defaults, TomlRecordConfig::new);

    if (parsed.defaults.topic == null) {
      parsed.defaults.topic = DEFAULT_TOPIC_NAME;
    }

    if (parsed.defaults.allowedTypes == null) {
      parsed.defaults.allowedTypes = DEFAULT_ALLOWED_TYPES;
    }

    parsed.typeMap.put(ValueType.DEPLOYMENT, parseOrDefault(parsed, config.deployment));
    parsed.typeMap.put(ValueType.INCIDENT, parseOrDefault(parsed, config.incident));
    parsed.typeMap.put(ValueType.JOB, parseOrDefault(parsed, config.job));
    parsed.typeMap.put(ValueType.JOB_BATCH, parseOrDefault(parsed, config.jobBatch));
    parsed.typeMap.put(ValueType.MESSAGE, parseOrDefault(parsed, config.message));
    parsed.typeMap.put(
        ValueType.MESSAGE_SUBSCRIPTION, parseOrDefault(parsed, config.messageSubscription));
    parsed.typeMap.put(
        ValueType.MESSAGE_START_EVENT_SUBSCRIPTION,
        parseOrDefault(parsed, config.messageStartEventSubscription));
    parsed.typeMap.put(ValueType.TIMER, parseOrDefault(parsed, config.timer));
    parsed.typeMap.put(ValueType.VARIABLE, parseOrDefault(parsed, config.variable));
    parsed.typeMap.put(
        ValueType.VARIABLE_DOCUMENT, parseOrDefault(parsed, config.variableDocument));
    parsed.typeMap.put(
        ValueType.WORKFLOW_INSTANCE, parseOrDefault(parsed, config.workflowInstance));
    parsed.typeMap.put(
        ValueType.WORKFLOW_INSTANCE_CREATION,
        parseOrDefault(parsed, config.workflowInstanceCreation));
    parsed.typeMap.put(
        ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION,
        parseOrDefault(parsed, config.workflowInstanceSubscription));

    return parsed;
  }

  private RecordConfig parseOrDefault(RecordsConfig recordsConfig, TomlRecordConfig config) {
    final RecordConfig parsed = recordConfigParser.parse(config, TomlRecordConfig::new);

    if (parsed.topic == null) {
      parsed.topic = recordsConfig.defaults.topic;
    }

    if (parsed.allowedTypes == null) {
      parsed.allowedTypes = recordsConfig.defaults.allowedTypes;
    }

    return parsed;
  }
}
