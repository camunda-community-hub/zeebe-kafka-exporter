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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * A {@link Deserializer} implementations for {@link RecordId} objects, which uses a pre-configured
 * * {@link ObjectReader} for that type.
 */
public final class RecordIdDeserializer extends JacksonDeserializer<RecordId> {
  public RecordIdDeserializer() {
    this(new ObjectMapper());
  }

  public RecordIdDeserializer(final ObjectMapper objectMapper) {
    this(objectMapper.readerFor(RecordId.class));
  }

  public RecordIdDeserializer(final ObjectReader objectReader) {
    super(objectReader);
  }
}
