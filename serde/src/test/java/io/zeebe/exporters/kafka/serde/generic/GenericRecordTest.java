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
package io.zeebe.exporters.kafka.serde.generic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.exporters.kafka.serde.SchemaDeserializationException;
import org.junit.Test;

public class GenericRecordTest {
  @Test
  public void shouldGetRecordMetadata() {
    // given
    final Schema.RecordMetadata metadata =
        Schema.RecordMetadata.newBuilder().setPosition(1L).build();
    final Schema.ErrorRecord record = Schema.ErrorRecord.newBuilder().setMetadata(metadata).build();
    final GenericRecord genericRecord = newRecord(record);

    // when
    final Schema.RecordMetadata reflectedMetadata = genericRecord.getMetadata();

    // then
    assertThat(reflectedMetadata.getPosition()).isEqualTo(1L);
  }

  @Test
  public void shouldThrowMissingExceptionIfNotARecordType() {
    // given
    final Message message = Any.newBuilder().build();
    final GenericRecord record = new GenericRecord(message, Any.getDescriptor().getFullName());

    // then
    assertThatThrownBy(record::getMetadata).isInstanceOf(SchemaDeserializationException.class);
  }

  @Test
  public void shouldGetMessageAsConcreteClass() {
    // given
    final Schema.MessageRecord messageRecord = Schema.MessageRecord.newBuilder().build();
    final GenericRecord record = newRecord(messageRecord);

    // when
    final Schema.MessageRecord converted = record.getMessageAs(messageRecord.getClass());

    // then
    assertThat(converted).isEqualTo(messageRecord);
  }

  private GenericRecord newRecord(Message message) {
    return new GenericRecord(message, message.getDescriptorForType().getName());
  }
}
