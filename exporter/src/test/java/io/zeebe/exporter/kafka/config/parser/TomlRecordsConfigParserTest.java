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
package io.zeebe.exporter.kafka.config.parser;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.exporter.kafka.config.RecordsConfig;
import io.zeebe.exporter.kafka.config.toml.TomlRecordConfig;
import io.zeebe.exporter.kafka.config.toml.TomlRecordsConfig;
import io.zeebe.exporter.kafka.record.AllowedType;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;

public class TomlRecordsConfigParserTest {
  private static final Set<ValueType> EXPECTED_VALUE_TYPES =
      EnumSet.complementOf(
          EnumSet.of(
              ValueType.EXPORTER, ValueType.NOOP, ValueType.NULL_VAL, ValueType.SBE_UNKNOWN));

  private final TomlRecordsConfigParser parser = new TomlRecordsConfigParser();

  @Test
  public void shouldParseDefaultsWithDefaultValue() {
    // given
    final TomlRecordsConfig config = new TomlRecordsConfig();

    // when
    final RecordsConfig parsed = parser.parse(config);

    // then
    assertThat(parsed.defaults.allowedTypes)
        .isEqualTo(TomlRecordsConfigParser.DEFAULT_ALLOWED_TYPES);
    assertThat(parsed.defaults.topic).isEqualTo(TomlRecordsConfigParser.DEFAULT_TOPIC_NAME);
  }

  @Test
  public void shouldParseRecordConfigUnderCorrectValueType() {
    // given
    final TomlRecordsConfig config = new TomlRecordsConfig();
    config.deployment = newConfigFromType(ValueType.DEPLOYMENT);
    config.incident = newConfigFromType(ValueType.INCIDENT);
    config.job = newConfigFromType(ValueType.JOB);
    config.jobBatch = newConfigFromType(ValueType.JOB_BATCH);
    config.message = newConfigFromType(ValueType.MESSAGE);
    config.messageSubscription = newConfigFromType(ValueType.MESSAGE_SUBSCRIPTION);
    config.messageStartEventSubscription =
        newConfigFromType(ValueType.MESSAGE_START_EVENT_SUBSCRIPTION);
    config.raft = newConfigFromType(ValueType.RAFT);
    config.timer = newConfigFromType(ValueType.TIMER);
    config.workflowInstance = newConfigFromType(ValueType.WORKFLOW_INSTANCE);
    config.workflowInstanceSubscription =
        newConfigFromType(ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION);

    // when
    final RecordsConfig parsed = parser.parse(config);

    // then
    for (final ValueType type : EXPECTED_VALUE_TYPES) {
      assertThat(parsed.forType(type).topic).isEqualTo(type.name());
    }
  }

  @Test
  public void shouldUseDefaultsOnMissingProperties() {
    // given
    final TomlRecordsConfig config = new TomlRecordsConfig();
    config.defaults = new TomlRecordConfig();
    config.defaults.topic = "default";
    config.defaults.type = Arrays.asList(AllowedType.COMMAND.name, AllowedType.REJECTION.name);

    // when
    final RecordsConfig parsed = parser.parse(config);

    // then
    parsed.typeMap.forEach(
        (t, c) -> {
          assertThat(c.topic).isEqualTo(config.defaults.topic);
          assertThat(c.allowedTypes)
              .containsExactly(RecordType.COMMAND, RecordType.COMMAND_REJECTION);
        });
  }

  private TomlRecordConfig newConfigFromType(ValueType type) {
    final TomlRecordConfig recordConfig = new TomlRecordConfig();
    recordConfig.topic = type.name();

    return recordConfig;
  }
}
