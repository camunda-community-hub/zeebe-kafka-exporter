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

import com.google.protobuf.Message;
import io.zeebe.protocol.record.Record;
import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Serializes a record to its equivalent Protobuf message.
 *
 * @param <R> concrete record type
 * @param <M> concrete message type
 */
public class SchemaSerializer<R extends Record, M extends Message> implements Serializer<R> {
  protected final SchemaTransformer<R, M> transformer;

  public SchemaSerializer(SchemaTransformer<R, M> transformer) {
    this.transformer = transformer;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // nothing to configure
  }

  @Override
  public byte[] serialize(String topic, R data) {
    return transformer.transform(data).toByteArray();
  }

  @Override
  public void close() {
    // nothing to close
  }
}
