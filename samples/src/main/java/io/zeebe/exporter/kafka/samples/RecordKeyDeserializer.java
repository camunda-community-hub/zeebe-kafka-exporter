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
package io.zeebe.exporter.kafka.samples;

import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class RecordKeyDeserializer implements Deserializer<RecordKey> {
  private final StringDeserializer deserializer = new StringDeserializer();

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    deserializer.configure(configs, isKey);
  }

  @Override
  public RecordKey deserialize(String topic, byte[] data) {
    final String key = deserializer.deserialize(topic, data);
    final String[] parts = key.split("-");

    return new RecordKey(Integer.valueOf(parts[0]), Long.valueOf(parts[1]));
  }

  @Override
  public void close() {
    deserializer.close();
  }
}
