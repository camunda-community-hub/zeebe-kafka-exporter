/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package io.zeebe.exporter.kafka.serialization;

import io.zeebe.exporter.record.Record;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public class RecordKeySerializer implements Serializer<Record> {
  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {}

  @Override
  public byte[] serialize(String topic, Record record) {
    final ByteBuffer buffer = newKeyBuffer();

    return buffer
        .order(ByteOrder.BIG_ENDIAN)
        .putInt(record.getMetadata().getPartitionId())
        .putLong(record.getKey())
        .array();
  }

  @Override
  public void close() {}

  private ByteBuffer newKeyBuffer() {
    return ByteBuffer.allocate(Long.BYTES + Integer.BYTES);
  }
}
