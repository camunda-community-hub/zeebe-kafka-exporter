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
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * A {@link Serializer} implementations for {@link RecordId} objects, which first uses a wrapped
 * {@link StringSerializer} to serialize {@link RecordId} to JSON. You can specify your encoding of
 * preference via {@link StringSerializer} configuration. Any configuration given to this serializer
 * is also passed to the wrapped {@link StringSerializer}.
 */
public final class RecordIdSerializer implements Serializer<RecordId> {
  private static final ObjectWriter WRITER = new ObjectMapper().writerFor(RecordId.class);
  private final StringSerializer delegate;

  public RecordIdSerializer() {
    this(new StringSerializer());
  }

  public RecordIdSerializer(final @NonNull StringSerializer delegate) {
    this.delegate = delegate;
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    delegate.configure(configs, isKey);
  }

  @Override
  public byte[] serialize(final String topic, final RecordId data) {
    try {
      return delegate.serialize(topic, WRITER.writeValueAsString(data));
    } catch (final JsonProcessingException e) {
      throw new SerializationException(e);
    }
  }

  @Override
  public void close() {
    delegate.close();
  }
}
