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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.exporters.kafka.serde.SchemaDeserializationException;
import io.zeebe.protocol.record.Record;
import java.util.Map;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Allows deserializing any record types provided the type name was correctly added to the headers
 * during serialization (see {@link GenericRecordSerializer#serialize(String, Headers, Record)}.
 *
 * <p>The returned object is a thin wrapper around the concrete message type (e.g.
 * Schema.DeploymentRecord, Schema.IncidentRecord), but its message property {@link
 * GenericRecord#getMessage()} is of the correct type, such that if it was a {@link
 * Schema.DeploymentRecord} then it would be safe to do the following:
 *
 * <pre>
 * final GenericRecord record;
 * // obtain an instance of the record
 * final Schema.DeploymentRecord withHelper = record.getMessageAs(Schema.DeploymentRecord.class);
 * final Schema.DeploymentRecord withCasting = (Schema.DeploymentRecord)record.getMessage();
 * // ...
 * </pre>
 */
public class GenericRecordDeserializer implements Deserializer<GenericRecord> {
  private static final String MISSING_DESCRIPTOR_HEADER_ERROR =
      "Cannot deserialize GenericRecord instances without specifying the descriptor header";

  private final String schemaDescriptorHeaderKey;
  private final StringDeserializer schemaDescriptorDeserializer;

  public GenericRecordDeserializer() {
    this(GenericRecordDescriptorHeader.DEFAULT_KEY);
  }

  public GenericRecordDeserializer(String schemaDescriptorHeaderKey) {
    this(schemaDescriptorHeaderKey, new StringDeserializer());
  }

  public GenericRecordDeserializer(
      String schemaDescriptorHeaderKey, StringDeserializer schemaDescriptorDeserializer) {
    this.schemaDescriptorHeaderKey = schemaDescriptorHeaderKey;
    this.schemaDescriptorDeserializer = schemaDescriptorDeserializer;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    schemaDescriptorDeserializer.configure(configs, isKey);
  }

  @Override
  public GenericRecord deserialize(String topic, Headers headers, byte[] data) {
    final Header header = headers.lastHeader(schemaDescriptorHeaderKey);
    final String descriptorName = schemaDescriptorDeserializer.deserialize(topic, header.value());
    final Message type = SchemaType.forType(descriptorName);
    final Message message;

    try {
      message = type.getParserForType().parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new SchemaDeserializationException(e);
    }

    return new GenericRecord(message, descriptorName);
  }

  @Override
  public GenericRecord deserialize(String topic, byte[] data) {
    throw new UnsupportedOperationException(MISSING_DESCRIPTOR_HEADER_ERROR);
  }

  @Override
  public void close() {
    schemaDescriptorDeserializer.close();
  }
}
