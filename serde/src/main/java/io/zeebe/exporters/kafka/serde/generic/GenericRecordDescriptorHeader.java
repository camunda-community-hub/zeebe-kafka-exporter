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

import org.apache.kafka.common.header.Header;

/** Header used to specify the record's type descriptor. */
public class GenericRecordDescriptorHeader implements Header {
  static final String DEFAULT_KEY =
      "io.zeebe.exporter.kafka.serde.generic.GenericRecordDescriptorHeader";

  private final String key;
  private final byte[] descriptor;

  public GenericRecordDescriptorHeader(byte[] descriptor) {
    this(DEFAULT_KEY, descriptor);
  }

  public GenericRecordDescriptorHeader(String key, byte[] descriptor) {
    this.key = key;
    this.descriptor = descriptor;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public byte[] value() {
    return descriptor;
  }
}
