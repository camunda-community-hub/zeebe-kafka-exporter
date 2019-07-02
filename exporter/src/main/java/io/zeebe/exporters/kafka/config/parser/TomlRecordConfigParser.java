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
package io.zeebe.exporters.kafka.config.parser;

import io.zeebe.exporters.kafka.config.RecordConfig;
import io.zeebe.exporters.kafka.config.toml.TomlRecordConfig;
import io.zeebe.exporters.kafka.record.AllowedType;
import io.zeebe.protocol.record.RecordType;
import java.util.EnumSet;

public class TomlRecordConfigParser implements ConfigParser<TomlRecordConfig, RecordConfig> {
  @Override
  public RecordConfig parse(TomlRecordConfig config) {
    final RecordConfig parsed = new RecordConfig();

    if (config.type != null) {
      parsed.setAllowedTypes(EnumSet.noneOf(RecordType.class));
      config.type.forEach(
          t -> parsed.getAllowedTypes().add(AllowedType.forName(t).getRecordType()));
    }

    if (config.topic != null) {
      parsed.setTopic(config.topic);
    }

    return parsed;
  }
}
