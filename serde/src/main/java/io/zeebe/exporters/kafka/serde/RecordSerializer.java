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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.camunda.zeebe.protocol.jackson.ZeebeProtocolModule;
import io.camunda.zeebe.protocol.record.Record;
import org.apache.kafka.common.serialization.Serializer;

/**
 * A {@link Serializer} implementations for {@link Record} objects which uses a pre-configured
 * {@link ObjectWriter} for that type.
 *
 * <p>NOTE: this serializer is not used by the exporter itself. The exporter uses a custom
 * serializer which piggybacks on Zeebe's built-in {@link Record#toJson()} method which does not
 * allow customization of the underlying {@link ObjectWriter}. It's provided here for testing
 * purposes, and potentially for users who would like to produce records to the same topics but
 * separately.
 */
public final class RecordSerializer extends JacksonSerializer<Record<?>> {
  public RecordSerializer() {
    this(new ObjectMapper().registerModule(new ZeebeProtocolModule()));
  }

  protected RecordSerializer(final ObjectMapper objectMapper) {
    this(objectMapper.writerFor(new TypeReference<Record<?>>() {}));
  }

  protected RecordSerializer(final ObjectWriter writer) {
    super(writer);
  }
}
