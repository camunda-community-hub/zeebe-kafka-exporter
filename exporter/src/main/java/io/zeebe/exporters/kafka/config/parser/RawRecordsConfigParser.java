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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.zeebe.exporters.kafka.config.RecordConfig;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordsConfig;
import io.zeebe.protocol.record.ValueType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link RawRecordsConfigParser} parses instances of {@link RawRecordsConfig} into valid instances
 * of {@link RecordsConfig}.
 *
 * <p>You'll note that it's not possible to pass your own implementation of {@code
 * ConfigParser<RawRecordConfig, RecordConfig>} - this is because after parsing {@link
 * RawRecordsConfig#defaults}, the result is passed as defaults to a new instance of {@link
 * RawRecordConfigParser}. This breaks the usual design and usage of DI, and should be refactored.
 */
public class RawRecordsConfigParser implements ConfigParser<RawRecordsConfig, RecordsConfig> {
  private static final ConfigParser<RawRecordConfig, RecordConfig> DEFAULTS_RECORD_CONFIG_PARSER =
      new RawRecordConfigParser();

  @SuppressWarnings("java:S138")
  @Override
  public @NonNull RecordsConfig parse(final @Nullable RawRecordsConfig config) {
    Objects.requireNonNull(config);

    final Map<ValueType, RecordConfig> typeMap = new EnumMap<>(ValueType.class);
    final RecordConfig defaults =
        DEFAULTS_RECORD_CONFIG_PARSER.parse(config.defaults, RawRecordConfig::new);
    final ConfigParser<RawRecordConfig, RecordConfig> recordConfigParser =
        new RawRecordConfigParser(defaults);

    Optional.ofNullable(config.deployment)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.DEPLOYMENT, c));
    Optional.ofNullable(config.error)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.ERROR, c));
    Optional.ofNullable(config.incident)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.INCIDENT, c));
    Optional.ofNullable(config.job)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.JOB, c));
    Optional.ofNullable(config.jobBatch)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.JOB_BATCH, c));
    Optional.ofNullable(config.message)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.MESSAGE, c));
    Optional.ofNullable(config.messageSubscription)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.MESSAGE_SUBSCRIPTION, c));
    Optional.ofNullable(config.messageStartEventSubscription)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.MESSAGE_START_EVENT_SUBSCRIPTION, c));
    Optional.ofNullable(config.timer)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.TIMER, c));
    Optional.ofNullable(config.variable)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.VARIABLE, c));
    Optional.ofNullable(config.variableDocument)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.VARIABLE_DOCUMENT, c));
    Optional.ofNullable(config.workflowInstance)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.WORKFLOW_INSTANCE, c));
    Optional.ofNullable(config.workflowInstanceCreation)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.WORKFLOW_INSTANCE_CREATION, c));
    Optional.ofNullable(config.workflowInstanceResult)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.WORKFLOW_INSTANCE_RESULT, c));
    Optional.ofNullable(config.workflowInstanceSubscription)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION, c));

    return new RecordsConfig(typeMap, defaults);
  }
}
