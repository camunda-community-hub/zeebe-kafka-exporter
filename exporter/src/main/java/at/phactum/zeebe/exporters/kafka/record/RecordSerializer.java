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
package at.phactum.zeebe.exporters.kafka.record;

import io.camunda.zeebe.protocol.record.Record;
import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * A {@link Serializer} implementations for {@link Record} objects, which first uses a wrapped
 * {@link StringSerializer} to serialize {@link Record} to JSON. You can specify your encoding of
 * preference via {@link StringSerializer} configuration. Any configuration given to this serializer
 * is also passed to the wrapped {@link StringSerializer}.
 */
public final class RecordSerializer implements Serializer<Record<?>> {
  private final StringSerializer delegate;

  public RecordSerializer() {
    this(new StringSerializer());
  }

  public RecordSerializer(final StringSerializer delegate) {
    this.delegate = delegate;
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    delegate.configure(configs, isKey);
  }

  @Override
  public byte[] serialize(final String topic, final Record data) {
    return delegate.serialize(topic, data.toJson());
  }

  @Override
  public void close() {
    delegate.close();
  }
}
