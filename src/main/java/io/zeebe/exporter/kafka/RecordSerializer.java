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
package io.zeebe.exporter.kafka;

import io.zeebe.exporter.record.Record;
import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Serializes a {@link Record} for consumption by Kafka.
 *
 * <p>If used as a key serializer (see {@link Serializer#configure(Map, boolean)}), serializes a
 * buffer containing first the partition ID, followed by the position. This is a unique combination
 * for a given Zeebe cluster, and should be enough to efficiently deduplicate records.
 *
 * <p>If used as a value serializer, it serializes the record to JSON and reuses Kafka's built-in
 * {@link StringSerializer}.
 */
public class RecordSerializer implements Serializer<Record> {
  // Will be formatted as partitionId-position
  private static final String KEY_FORMAT = "%d-%d";

  private final StringSerializer serializer = new StringSerializer();
  private boolean isKeySerializer;

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    isKeySerializer = isKey;
  }

  @Override
  public byte[] serialize(String topic, Record record) {
    final String serialized;

    if (isKeySerializer) {
      serialized = serializeKey(record);
    } else {
      serialized = serializeValue(record);
    }

    return serializer.serialize(topic, serialized);
  }

  @Override
  public void close() {
    serializer.close();
  }

  private String serializeKey(Record record) {
    return String.format(KEY_FORMAT, record.getMetadata().getPartitionId(), record.getPosition());
  }

  private String serializeValue(Record record) {
    return record.toJson();
  }
}
