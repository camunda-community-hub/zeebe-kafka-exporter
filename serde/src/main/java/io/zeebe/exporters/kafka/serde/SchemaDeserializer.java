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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Deserializes a specific Protobuf message from a given parser.
 *
 * @param <T> concrete message type
 */
public class SchemaDeserializer<T extends Message> implements Deserializer<T> {
  private final Parser<T> parser;

  public SchemaDeserializer(Parser<T> parser) {
    this.parser = parser;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // nothing to configure
  }

  @Override
  public T deserialize(String topic, byte[] data) {
    try {
      return parser.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new SchemaDeserializationException(e);
    }
  }

  @Override
  public void close() {
    // nothing to close
  }
}
