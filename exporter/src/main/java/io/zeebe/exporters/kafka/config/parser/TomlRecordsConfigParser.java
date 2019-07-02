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
import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import java.util.EnumSet;
import java.util.Map;

public class TomlRecordsConfigParser implements ConfigParser<TomlRecordsConfig, RecordsConfig> {
  static final String DEFAULT_TOPIC_NAME = "zeebe";
  static final EnumSet<RecordType> DEFAULT_ALLOWED_TYPES =
      EnumSet.complementOf(EnumSet.of(RecordType.NULL_VAL, RecordType.SBE_UNKNOWN));

  private final ConfigParser<TomlRecordConfig, RecordConfig> recordConfigParser;

  TomlRecordsConfigParser() {
    this.recordConfigParser = new TomlRecordConfigParser();
  }

  public TomlRecordsConfigParser(ConfigParser<TomlRecordConfig, RecordConfig> recordConfigParser) {
    this.recordConfigParser = recordConfigParser;
  }

  @Override
  public RecordsConfig parse(TomlRecordsConfig config) {
    final RecordsConfig parsed = new RecordsConfig();
    final RecordConfig defaults = recordConfigParser.parse(config.defaults, TomlRecordConfig::new);
    final Map<ValueType, RecordConfig> typeMap = parsed.getTypeMap();

    parsed.setDefaults(defaults);
    if (defaults.getTopic() == null) {
      defaults.setTopic(DEFAULT_TOPIC_NAME);
    }

    if (defaults.getAllowedTypes() == null) {
      defaults.setAllowedTypes(DEFAULT_ALLOWED_TYPES);
    }

    typeMap.put(ValueType.DEPLOYMENT, parseOrDefault(defaults, config.deployment));
    typeMap.put(ValueType.ERROR, parseOrDefault(defaults, config.error));
    typeMap.put(ValueType.INCIDENT, parseOrDefault(defaults, config.incident));
    typeMap.put(ValueType.JOB, parseOrDefault(defaults, config.job));
    typeMap.put(ValueType.JOB_BATCH, parseOrDefault(defaults, config.jobBatch));
    typeMap.put(ValueType.MESSAGE, parseOrDefault(defaults, config.message));
    typeMap.put(
        ValueType.MESSAGE_SUBSCRIPTION, parseOrDefault(defaults, config.messageSubscription));
    typeMap.put(
        ValueType.MESSAGE_START_EVENT_SUBSCRIPTION,
        parseOrDefault(defaults, config.messageStartEventSubscription));
    typeMap.put(ValueType.TIMER, parseOrDefault(defaults, config.timer));
    typeMap.put(ValueType.VARIABLE, parseOrDefault(defaults, config.variable));
    typeMap.put(ValueType.VARIABLE_DOCUMENT, parseOrDefault(defaults, config.variableDocument));
    typeMap.put(ValueType.WORKFLOW_INSTANCE, parseOrDefault(defaults, config.workflowInstance));
    typeMap.put(
        ValueType.WORKFLOW_INSTANCE_CREATION,
        parseOrDefault(defaults, config.workflowInstanceCreation));
    typeMap.put(
        ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION,
        parseOrDefault(defaults, config.workflowInstanceSubscription));

    return parsed;
  }

  private RecordConfig parseOrDefault(RecordConfig defaults, TomlRecordConfig config) {
    final RecordConfig parsed = recordConfigParser.parse(config, TomlRecordConfig::new);

    if (parsed.getTopic() == null) {
      parsed.setTopic(defaults.getTopic());
    }

    if (parsed.getAllowedTypes() == null) {
      parsed.setAllowedTypes(defaults.getAllowedTypes());
    }

    return parsed;
  }
}
