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

import com.github.daniel.shuy.kafka.protobuf.serde.KafkaProtobufSerializer;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.exporter.proto.RecordTransformer;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.protocol.record.Record;
import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;

/**
 * A {@link Serializer} implementations for {@link Record} objects which uses
 * KafkaProtobufSerializer to serialise Schema.Record instances to kafak
 */
@SuppressWarnings("rawtypes")
public final class ProtobufRecordSerializer implements Serializer<Record> {
  private final KafkaProtobufSerializer<Schema.Record> delegate;

  public ProtobufRecordSerializer() {
    this(new KafkaProtobufSerializer<Schema.Record>());
  }

  public ProtobufRecordSerializer(final @NonNull KafkaProtobufSerializer<Schema.Record> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    delegate.configure(configs, isKey);
  }

  @Override
  public byte[] serialize(final String topic, final Record data) {
    return delegate.serialize(topic, RecordTransformer.toGenericRecord(data));
  }

  @Override
  public void close() {
    delegate.close();
  }
}
