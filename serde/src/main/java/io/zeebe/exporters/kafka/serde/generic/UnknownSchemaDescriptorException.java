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
package io.zeebe.exporters.kafka.serde.generic;

import com.google.protobuf.Descriptors;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.exporters.kafka.serde.SchemaDeserializationException;
import java.util.stream.Collectors;

/** Thrown when trying to deserialize a message from an unknown descriptor. */
public class UnknownSchemaDescriptorException extends SchemaDeserializationException {
  private static final String DEFAULT_FORMAT;

  static {
    final String knownDescriptors =
        Schema.getDescriptor()
            .getMessageTypes()
            .stream()
            .map(Descriptors.Descriptor::getFullName)
            .collect(Collectors.joining(", "));

    DEFAULT_FORMAT =
        "Unknown schema descriptor name '%s', expected one of: [" + knownDescriptors + "]";
  }

  public UnknownSchemaDescriptorException(String descriptorName) {
    super(String.format(DEFAULT_FORMAT, descriptorName));
  }

  public UnknownSchemaDescriptorException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnknownSchemaDescriptorException(Throwable cause) {
    super(cause);
  }
}
