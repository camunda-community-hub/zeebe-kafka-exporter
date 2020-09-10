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

import com.github.daniel.shuy.kafka.protobuf.serde.KafkaProtobufDeserializer;
import com.google.protobuf.InvalidProtocolBufferException;
import io.zeebe.exporter.proto.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * A {@link Deserializer} implementations for {@link com.google.protobuf.Message} objects. It
 * expects the records to be encoded in protobuf format as per ProtobufRecordSerializer
 *
 * <p>The records are dezerialised and the unpacked into type specific proto buf records e.g.
 * Schema.DeploymentRecord
 */
public final class ProtobufRecordDeserializer implements Deserializer<com.google.protobuf.Message> {

  private static final List<Class<? extends com.google.protobuf.Message>> RECORD_MESSAGE_TYPES;

  static {
    RECORD_MESSAGE_TYPES = new ArrayList<>();
    RECORD_MESSAGE_TYPES.add(Schema.DeploymentRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.JobRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.JobBatchRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.ErrorRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.VariableRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.VariableDocumentRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.MessageStartEventSubscriptionRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.MessageSubscriptionRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.MessageRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.WorkflowInstanceRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.WorkflowInstanceCreationRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.WorkflowInstanceResultRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.WorkflowInstanceSubscriptionRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.TimerRecord.class);
    RECORD_MESSAGE_TYPES.add(Schema.IncidentRecord.class);
  }

  private static final KafkaProtobufDeserializer<Schema.Record> PROTOBUF_DESERIALIZER =
      new KafkaProtobufDeserializer<>(Schema.Record.parser());

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {}

  @Override
  public com.google.protobuf.Message deserialize(final String topic, final byte[] data) {
    final Schema.Record record = PROTOBUF_DESERIALIZER.deserialize(topic, data);
    return unpackRecord(record);
  }

  private com.google.protobuf.Message unpackRecord(Schema.Record record) {
    for (Class<? extends com.google.protobuf.Message> type : RECORD_MESSAGE_TYPES) {
      if (record.getRecord().is(type)) {
        try {
          return record.getRecord().unpack(type);
        } catch (InvalidProtocolBufferException e) {
          throw new SerializationException(e);
        }
      }
    }
    throw new SerializationException("Unrecognised record type");
  }

  @Override
  public void close() {}
}
