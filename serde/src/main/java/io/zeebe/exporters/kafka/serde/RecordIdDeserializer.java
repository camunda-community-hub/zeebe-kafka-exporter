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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * A {@link Deserializer} implementations for {@link RecordId} objects, which first uses a wrapped
 * {@link StringDeserializer} to deserialize bytes into a JSON string. This is done to accomodate
 * different byte encodings which you can configure via the {@link StringDeserializer}. If you
 * configure a non standard byte encoding, make sure that you do the same on the serializer and the
 * deserializer.
 */
public final class RecordIdDeserializer implements Deserializer<RecordId> {
  private static final ObjectReader READER = new ObjectMapper().readerFor(RecordId.class);
  private final StringDeserializer delegate;

  public RecordIdDeserializer() {
    this(new StringDeserializer());
  }

  public RecordIdDeserializer(final @NonNull StringDeserializer delegate) {
    this.delegate = delegate;
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    delegate.configure(configs, isKey);
  }

  @Override
  public RecordId deserialize(final String topic, final byte[] data) {
    final String decoded = delegate.deserialize(topic, data);
    try {
      return READER.readValue(decoded);
    } catch (final JsonProcessingException e) {
      throw new SerializationException(
          String.format(
              "Expected to deserialize JSON data from topic [%s] into a RecordId instance, but failed",
              topic),
          e);
    }
  }

  @Override
  public void close() {
    delegate.close();
  }
}
