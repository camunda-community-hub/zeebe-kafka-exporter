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

import io.camunda.zeebe.protocol.record.ValueType;
import io.zeebe.exporters.kafka.config.RecordConfig;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordsConfig;
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
  public RecordsConfig parse(final RawRecordsConfig config) {
    Objects.requireNonNull(config);

    final Map<ValueType, RecordConfig> typeMap = new EnumMap<>(ValueType.class);
    final RecordConfig defaults =
        DEFAULTS_RECORD_CONFIG_PARSER.parse(config.defaults, RawRecordConfig::new);
    final ConfigParser<RawRecordConfig, RecordConfig> recordConfigParser =
        new RawRecordConfigParser(defaults);

    Optional.ofNullable(config.deployment)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.DEPLOYMENT, c));
    Optional.ofNullable(config.deploymentDistribution)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.DEPLOYMENT_DISTRIBUTION, c));
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
    Optional.ofNullable(config.processInstance)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.PROCESS_INSTANCE, c));
    Optional.ofNullable(config.processInstanceCreation)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.PROCESS_INSTANCE_CREATION, c));
    Optional.ofNullable(config.processInstanceResult)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.PROCESS_INSTANCE_RESULT, c));
    Optional.ofNullable(config.processMessageSubscription)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.PROCESS_MESSAGE_SUBSCRIPTION, c));
    Optional.ofNullable(config.process)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.PROCESS, c));
    Optional.ofNullable(config.processEvent)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.PROCESS_EVENT, c));
    Optional.ofNullable(config.timer)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.TIMER, c));
    Optional.ofNullable(config.variable)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.VARIABLE, c));
    Optional.ofNullable(config.variableDocument)
        .map(recordConfigParser::parse)
        .ifPresent(c -> typeMap.put(ValueType.VARIABLE_DOCUMENT, c));
    Optional.ofNullable(config.decision)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.DECISION, c));
    Optional.ofNullable(config.decisionRequirements)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.DECISION_REQUIREMENTS, c));
    Optional.ofNullable(config.decisionEvaluation)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.DECISION_EVALUATION, c));
    Optional.ofNullable(config.processInstanceModification)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.PROCESS_INSTANCE_MODIFICATION, c));
    Optional.ofNullable(config.escalation)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.ESCALATION, c));
    Optional.ofNullable(config.signalSubscription)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.SIGNAL_SUBSCRIPTION, c));
    Optional.ofNullable(config.signal)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.SIGNAL, c));
    Optional.ofNullable(config.resourceDeletion)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.RESOURCE_DELETION, c));
    Optional.ofNullable(config.commandDistribution)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.COMMAND_DISTRIBUTION, c));
    Optional.ofNullable(config.processInstanceBatch)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.PROCESS_INSTANCE_BATCH, c));
    Optional.ofNullable(config.checkpoint)
            .map(recordConfigParser::parse)
            .ifPresent(c -> typeMap.put(ValueType.CHECKPOINT, c));
    return new RecordsConfig(typeMap, defaults);
  }
}
