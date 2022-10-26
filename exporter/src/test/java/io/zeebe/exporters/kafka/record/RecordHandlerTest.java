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
package io.zeebe.exporters.kafka.record;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.protocol.jackson.ZeebeProtocolModule;
import io.camunda.zeebe.protocol.record.ImmutableRecord;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.DeploymentIntent;
import io.camunda.zeebe.protocol.record.value.ImmutableDeploymentRecordValue;
import io.zeebe.exporters.kafka.config.RecordConfig;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.exporters.kafka.serde.RecordId;
import java.util.EnumSet;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
final class RecordHandlerTest {

  private static final RecordConfig DEFAULT_RECORD_CONFIG =
      new RecordConfig(EnumSet.allOf(RecordType.class), "zeebe");

  @Test
  void shouldTransformRecord() throws JsonProcessingException {
    // given
    final ObjectMapper mapper = new ObjectMapper().registerModule(new ZeebeProtocolModule());
    final Record<?> record = buildDeploymentRecord().withRecordType(RecordType.COMMAND).build();
    final RecordConfig deploymentRecordConfig =
        new RecordConfig(EnumSet.allOf(RecordType.class), "topic");
    final RecordHandler recordHandler = new RecordHandler(newRecordsConfig(RecordType.COMMAND));

    // when
    final ProducerRecord<RecordId, byte[]> transformed = recordHandler.transform(record);

    // then
    assertThat(transformed.topic()).isEqualTo(deploymentRecordConfig.getTopic());
    assertThat(transformed.key())
        .isEqualTo(new RecordId(record.getPartitionId(), record.getPosition()));
    assertThat(transformed.value()).isEqualTo(mapper.writeValueAsBytes(record));
  }

  @Test
  void shouldTestRecordAsNotAllowed() {
    // given
    final Record<?> record = buildDeploymentRecord().withRecordType(RecordType.COMMAND).build();
    final RecordHandler recordHandler = new RecordHandler(newRecordsConfig(RecordType.EVENT));

    // when - then
    assertThat(recordHandler.isAllowed(record)).isFalse();
  }

  @Test
  void shouldTestRecordAsAllowed() {
    // given
    final Record<?> record = buildDeploymentRecord().withRecordType(RecordType.EVENT).build();
    final RecordHandler recordHandler = new RecordHandler(newRecordsConfig(RecordType.EVENT));

    // when - then
    assertThat(recordHandler.isAllowed(record)).isTrue();
  }

  private RecordsConfig newRecordsConfig(final RecordType allowedType) {
    final RecordConfig recordConfig = new RecordConfig(EnumSet.of(allowedType), "topic");
    return new RecordsConfig(Map.of(ValueType.DEPLOYMENT, recordConfig), DEFAULT_RECORD_CONFIG);
  }

  private ImmutableRecord.Builder<?> buildDeploymentRecord() {
    return ImmutableRecord.builder()
      .withIntent(DeploymentIntent.CREATE)
      .withRecordType(RecordType.EVENT)
      .withValueType(ValueType.DEPLOYMENT)
      .withPartitionId(1)
      .withPosition(1)
      .withValue(ImmutableDeploymentRecordValue
        .builder()
        .build());
  }
}
