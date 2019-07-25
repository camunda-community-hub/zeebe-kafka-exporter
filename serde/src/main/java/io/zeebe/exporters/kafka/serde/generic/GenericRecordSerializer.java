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

import com.google.protobuf.Message;
import io.zeebe.exporter.proto.RecordTransformer;
import io.zeebe.exporters.kafka.serde.SchemaDeserializer;
import io.zeebe.exporters.kafka.serde.SchemaSerializer;
import io.zeebe.exporters.kafka.serde.SchemaTransformer;
import io.zeebe.protocol.record.Record;
import java.util.Map;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Serializes a {@link Record} such that it can be read by either {@link GenericRecordDeserializer}
 * or by a normal {@link SchemaDeserializer}.
 *
 * <p>In other words, it can be used to serialize two different record types {@code
 * Record<DeploymentRecordValue>} and {@code Record<IncidentRecordValue>} to two different topics,
 * say {@code zeebe-deployments} and {@code zeebe-incidents}. One could then read from {@code
 * zeebe-deployments} using a standard {@code SchemaDeserializer<Schema.DeploymentRecord>} which
 * returns directly a {@code Schema.DeploymentRecord}, and also read from both topic with a single
 * consumer by using a {@code GenericRecordDeserializer}, which will deserialize the messages to
 * their correct concrete types (i.e. {@code Schema.DeploymentRecord} and {@code
 * Schema.IncidentRecord}) using the record headers.
 */
public class GenericRecordSerializer extends SchemaSerializer<Record, Message> {
  private static final String MISSING_HEADER_DESCRIPTOR_ERROR =
      "Cannot serialize GenericRecord instances without specifying the descriptor header";
  private final StringSerializer schemaDescriptorSerializer;

  public GenericRecordSerializer() {
    this(new StringSerializer());
  }

  public GenericRecordSerializer(StringSerializer schemaDescriptorSerializer) {
    this(RecordTransformer::toProtobufMessage, schemaDescriptorSerializer);
  }

  public GenericRecordSerializer(
      SchemaTransformer<Record, Message> transformer, StringSerializer schemaDescriptorSerializer) {
    super(transformer);
    this.schemaDescriptorSerializer = schemaDescriptorSerializer;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    super.configure(configs, isKey);
    schemaDescriptorSerializer.configure(configs, isKey);
  }

  @Override
  public byte[] serialize(String topic, Headers headers, Record data) {
    final Message transformed = transformer.transform(data);
    final GenericRecordDescriptorHeader descriptorHeader =
        new GenericRecordDescriptorHeader(
            schemaDescriptorSerializer.serialize(topic, getDescriptorHeaderValue(transformed)));
    headers.add(descriptorHeader);

    return transformed.toByteArray();
  }

  @Override
  public byte[] serialize(String topic, Record data) {
    throw new UnsupportedOperationException(MISSING_HEADER_DESCRIPTOR_ERROR);
  }

  @Override
  public void close() {
    super.close();
    schemaDescriptorSerializer.close();
  }

  private String getDescriptorHeaderValue(Message message) {
    return message.getDescriptorForType().getName();
  }
}
