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

import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Deserializer implementation which reads an object from a pre-configured {@link ObjectReader}.
 *
 * @param <T> the concrete type to deserialize
 */
public abstract class JacksonDeserializer<T> implements Deserializer<T> {
  protected final ObjectReader reader;

  protected JacksonDeserializer(final ObjectReader reader) {
    this.reader = reader;
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {}

  @Override
  public T deserialize(final String topic, final byte[] data) {
    try {
      return reader.readValue(data);
    } catch (final IOException e) {
      throw new SerializationException(
          String.format("Expected to deserialize data from topic [%s], but failed", topic), e);
    }
  }
}
