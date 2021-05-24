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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Serializer implementation which writes an object from a pre-configured {@link ObjectWriter}.
 *
 * @param <T> the concrete type to serialize
 */
public abstract class JacksonSerializer<T> implements Serializer<T> {
  protected final ObjectWriter writer;

  protected JacksonSerializer(final ObjectWriter writer) {
    this.writer = writer;
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {}

  @Override
  public byte[] serialize(final String topic, final T data) {
    try {
      return writer.writeValueAsBytes(data);
    } catch (final JsonProcessingException e) {
      throw new SerializationException(
          String.format("Expected to serialize data for topic [%s], but failed", topic), e);
    }
  }

  @Override
  public void close() {}
}
