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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.exporters.kafka.serde.SchemaDeserializationException;

/**
 * Knows how to unpack a given message to a more concrete schema type based on the given type name.
 */
public class GenericRecord {
  private static final String NO_RECORD_METADATA_ERROR =
      "No RecordMetadata field on Protobuf message with type '%s', but all record types should have it";
  private final Descriptors.Descriptor descriptor;
  private final Message message;
  private final String typeName;

  private Schema.RecordMetadata metadata;

  public GenericRecord(Message message, String typeName) {
    this.message = message;
    this.typeName = typeName;
    this.descriptor = message.getDescriptorForType();
  }

  public <T extends Message> T getMessageAs(Class<T> clazz) {
    return clazz.cast(message);
  }

  public Message getMessage() {
    return message;
  }

  public String getTypeName() {
    return typeName;
  }

  public Schema.RecordMetadata getMetadata() {
    if (metadata == null) {
      metadata = extractMetadata();
    }

    return metadata;
  }

  private Schema.RecordMetadata extractMetadata() {
    final Descriptors.FieldDescriptor fieldDescriptor = descriptor.findFieldByName("metadata");
    if (fieldDescriptor != null) {
      final Object rawField = message.getField(fieldDescriptor);
      if (rawField instanceof Schema.RecordMetadata) {
        return (Schema.RecordMetadata) rawField;
      }
    }

    throw new SchemaDeserializationException(String.format(NO_RECORD_METADATA_ERROR, typeName));
  }
}
