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

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordsConfig;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
final class RawRecordsConfigParserTest {
  private static final Set<ValueType> EXPECTED_VALUE_TYPES =
      EnumSet.complementOf(EnumSet.of(ValueType.NULL_VAL, ValueType.SBE_UNKNOWN));

  private final RawRecordsConfigParser parser = new RawRecordsConfigParser();

  @Test
  void shouldParseDefaultsWithDefaultValue() {
    // given
    final RawRecordsConfig config = new RawRecordsConfig();

    // when
    final RecordsConfig parsed = parser.parse(config);

    // then
    assertThat(parsed.getDefaults().getAllowedTypes())
        .isEqualTo(RawRecordConfigParser.DEFAULT_ALLOWED_TYPES);
    assertThat(parsed.getDefaults().getTopic()).isEqualTo(RawRecordConfigParser.DEFAULT_TOPIC_NAME);
  }

  @Test
  void shouldParseRecordConfigUnderCorrectValueType() {
    // given
    final RawRecordsConfig config = new RawRecordsConfig();
    config.deployment = newConfigFromType(ValueType.DEPLOYMENT);
    config.deploymentDistribution = newConfigFromType(ValueType.DEPLOYMENT_DISTRIBUTION);
    config.error = newConfigFromType(ValueType.ERROR);
    config.incident = newConfigFromType(ValueType.INCIDENT);
    config.job = newConfigFromType(ValueType.JOB);
    config.jobBatch = newConfigFromType(ValueType.JOB_BATCH);
    config.message = newConfigFromType(ValueType.MESSAGE);
    config.messageSubscription = newConfigFromType(ValueType.MESSAGE_SUBSCRIPTION);
    config.messageStartEventSubscription =
        newConfigFromType(ValueType.MESSAGE_START_EVENT_SUBSCRIPTION);
    config.process = newConfigFromType(ValueType.PROCESS);
    config.processEvent = newConfigFromType(ValueType.PROCESS_EVENT);
    config.processInstance = newConfigFromType(ValueType.PROCESS_INSTANCE);
    config.processInstanceCreation = newConfigFromType(ValueType.PROCESS_INSTANCE_CREATION);
    config.processInstanceResult = newConfigFromType(ValueType.PROCESS_INSTANCE_RESULT);
    config.processMessageSubscription = newConfigFromType(ValueType.PROCESS_MESSAGE_SUBSCRIPTION);
    config.timer = newConfigFromType(ValueType.TIMER);
    config.variable = newConfigFromType(ValueType.VARIABLE);
    config.variableDocument = newConfigFromType(ValueType.VARIABLE_DOCUMENT);
    config.decision = newConfigFromType(ValueType.DECISION);
    config.decisionRequirements = newConfigFromType(ValueType.DECISION_REQUIREMENTS);
    config.decisionEvaluation = newConfigFromType(ValueType.DECISION_EVALUATION);
    config.processInstanceModification = newConfigFromType(ValueType.PROCESS_INSTANCE_MODIFICATION);
    config.escalation = newConfigFromType(ValueType.ESCALATION);
    config.signalSubscription = newConfigFromType(ValueType.SIGNAL_SUBSCRIPTION);
    config.signal = newConfigFromType(ValueType.SIGNAL);
    config.resourceDeletion = newConfigFromType(ValueType.RESOURCE_DELETION);
    config.commandDistribution = newConfigFromType(ValueType.COMMAND_DISTRIBUTION);
    config.processInstanceBatch = newConfigFromType(ValueType.PROCESS_INSTANCE_BATCH);
    config.checkpoint = newConfigFromType(ValueType.CHECKPOINT);
    // when
    final RecordsConfig parsed = parser.parse(config);

    // then
    for (final ValueType type : EXPECTED_VALUE_TYPES) {
      assertThat(parsed.forType(type).getTopic()).isEqualTo(type.name());
    }
  }

  @Test
  void shouldUseDefaultsOnMissingProperties() {
    // given
    final RawRecordsConfig config = new RawRecordsConfig();
    config.defaults = new RawRecordConfig();
    config.defaults.topic = "default";
    config.defaults.type =
        String.format(
            "%s,%s", AllowedType.COMMAND.getTypeName(), AllowedType.REJECTION.getTypeName());

    // when
    final RecordsConfig parsed = parser.parse(config);

    // then
    parsed
        .getTypeMap()
        .forEach(
            (t, c) -> {
              assertThat(c.getTopic()).isEqualTo(config.defaults.topic);
              assertThat(c.getAllowedTypes())
                  .containsExactly(RecordType.COMMAND, RecordType.COMMAND_REJECTION);
            });
  }

  private RawRecordConfig newConfigFromType(final ValueType type) {
    final RawRecordConfig recordConfig = new RawRecordConfig();
    recordConfig.topic = type.name();

    return recordConfig;
  }
}
