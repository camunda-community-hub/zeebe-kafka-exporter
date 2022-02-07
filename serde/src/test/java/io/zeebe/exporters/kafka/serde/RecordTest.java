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
package io.zeebe.exporters.kafka.serde;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import io.camunda.zeebe.protocol.jackson.record.DeploymentRecordValueBuilder;
import io.camunda.zeebe.protocol.jackson.record.RecordBuilder;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.DeploymentIntent;
import io.camunda.zeebe.protocol.record.value.DeploymentRecordValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
final class RecordTest {
  private static final String TOPIC = "zeebe";

  @Test
  void shouldSerialize() {
    // given
    final Record<?> record =
        new RecordBuilder<DeploymentRecordValue>()
            .intent(DeploymentIntent.CREATED)
            .recordType(RecordType.EVENT)
            .valueType(ValueType.DEPLOYMENT)
            .value(new DeploymentRecordValueBuilder().build())
            .build();
    final RecordSerializer serializer = new RecordSerializer();
    final RecordDeserializer deserializer = new RecordDeserializer();

    // when
    final byte[] serialized = serializer.serialize(TOPIC, record);
    final Record<?> deserialized = deserializer.deserialize(TOPIC, serialized);

    // then
    assertThat(deserialized)
        .as("the deserialized record is the same as the original")
        .isEqualTo(record);
  }

  @Test
  void shouldSerializeOtherFormat() {
    // given
    final ObjectMapper cborMapper = new CBORMapper();
    final Record<?> record =
        new RecordBuilder<DeploymentRecordValue>()
            .intent(DeploymentIntent.CREATED)
            .recordType(RecordType.EVENT)
            .valueType(ValueType.DEPLOYMENT)
            .value(new DeploymentRecordValueBuilder().build())
            .build();
    final RecordSerializer serializer = new RecordSerializer(cborMapper);
    final RecordDeserializer deserializer = new RecordDeserializer(cborMapper);

    // when
    final byte[] serialized = serializer.serialize(TOPIC, record);
    final Record<?> deserialized = deserializer.deserialize(TOPIC, serialized);

    // then
    assertThat(deserialized)
        .as("the deserialized record is the same as the original")
        .isEqualTo(record);
  }
}
