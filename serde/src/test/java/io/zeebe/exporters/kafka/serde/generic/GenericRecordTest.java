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
import io.zeebe.exporters.kafka.serde.util.SchemaFactory;
import org.junit.Test;

public class GenericRecordTest {
  private final SchemaFactory factory = new SchemaFactory();

  @Test
  public void shouldGetRecordMetadata() {
    // given
    final GenericRecord record = newRecord(factory.workflowInstance().build());

    // when
    final Schema.RecordMetadata metadata = record.getMetadata();

    // then
    assertThat(metadata.getPosition()).isEqualTo(1L);
  }

  @Test
  public void shouldThrowMissingExceptionIfNotARecordType() {
    // given
    final Message message = Any.newBuilder().build();
    final GenericRecord record = new GenericRecord(message, Any.getDescriptor().getFullName());

    // then
    assertThatThrownBy(record::getMetadata).isInstanceOf(MissingRecordMetadataException.class);
  }

  @Test
  public void shouldGetMessageAsConcreteClass() {
    // given
    final Schema.MessageRecord messageRecord = factory.message().build();
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
