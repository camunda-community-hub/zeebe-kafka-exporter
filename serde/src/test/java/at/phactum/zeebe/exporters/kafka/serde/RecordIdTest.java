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
package at.phactum.zeebe.exporters.kafka.serde;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
final class RecordIdTest {
  private static final String TOPIC = "zeebe";

  @Test
  void shouldSerialize() {
    // given
    final RecordId id = new RecordId(1, 1);
    final RecordIdSerializer serializer = new RecordIdSerializer();
    final RecordIdDeserializer deserializer = new RecordIdDeserializer();

    // when
    final byte[] serialized = serializer.serialize(TOPIC, id);
    final RecordId deserialized = deserializer.deserialize(TOPIC, serialized);

    // then
    assertThat(deserialized).as("the deserialized ID is the same as the original").isEqualTo(id);
  }

  @Test
  void shouldSerializeOtherFormat() {
    // given
    final ObjectMapper cborMapper = new CBORMapper();
    final RecordId id = new RecordId(1, 1);
    final RecordIdSerializer serializer = new RecordIdSerializer(cborMapper);
    final RecordIdDeserializer deserializer = new RecordIdDeserializer(cborMapper);

    // when
    final byte[] serialized = serializer.serialize(TOPIC, id);
    final RecordId deserialized = deserializer.deserialize(TOPIC, serialized);

    // then
    assertThat(deserialized).as("the deserialized ID is the same as the original").isEqualTo(id);
  }
}
