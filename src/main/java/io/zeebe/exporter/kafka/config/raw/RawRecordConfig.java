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
package io.zeebe.exporter.kafka.config.raw;

import io.zeebe.exporter.kafka.config.AllowedType;
import io.zeebe.exporter.kafka.config.RecordConfig;
import io.zeebe.protocol.clientapi.RecordType;
import java.util.EnumSet;
import java.util.List;

public class RawRecordConfig {
  public List<String> type;
  public String topic;

  public RecordConfig parse(RecordConfig defaults) {
    final RecordConfig parsed = new RecordConfig();

    if (type != null) {
      parsed.allowedTypes = EnumSet.noneOf(RecordType.class);
      type.forEach(t -> parsed.allowedTypes.add(AllowedType.forName(t).recordType));
    } else if (defaults != null) {
      parsed.allowedTypes = defaults.allowedTypes;
    }

    if (topic != null) {
      parsed.topic = topic;
    } else if (defaults != null) {
      parsed.topic = defaults.topic;
    }

    return parsed;
  }
}
