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

import static io.zeebe.exporters.kafka.config.parser.ConfigParserUtil.get;

import io.camunda.zeebe.protocol.record.RecordType;
import io.zeebe.exporters.kafka.config.RecordConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordConfig;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * {@link RawRecordConfigParser} parses instances of {@link RawRecordConfig} into valid instances of
 * {@link RecordConfig}, substituting defaults for missing properties.
 *
 * <p>The defaults can be overridden with an instance of {@link RecordConfig}, and default
 * properties will be taken from there. This is used notably in {@link RawRecordsConfigParser} where
 * it first parses {@link io.zeebe.exporters.kafka.config.raw.RawRecordsConfig#defaults} and passes
 * the parsed value as a defaults here for all subsequent properties.
 */
public class RawRecordConfigParser implements ConfigParser<RawRecordConfig, RecordConfig> {
  static final String DEFAULT_TOPIC_NAME = "zeebe";
  static final EnumSet<RecordType> DEFAULT_ALLOWED_TYPES =
      EnumSet.complementOf(EnumSet.of(RecordType.NULL_VAL, RecordType.SBE_UNKNOWN));

  private final RecordConfig defaults;

  public RawRecordConfigParser() {
    this(new RecordConfig(DEFAULT_ALLOWED_TYPES, DEFAULT_TOPIC_NAME));
  }

  public RawRecordConfigParser(final RecordConfig defaults) {
    this.defaults = defaults;
  }

  @Override
  public RecordConfig parse(final RawRecordConfig config) {
    Objects.requireNonNull(config);

    final Set<RecordType> allowedTypes;
    final String topic = Optional.ofNullable(config.topic).orElse(defaults.getTopic());

    if (config.type != null) {
      allowedTypes = EnumSet.noneOf(RecordType.class);
      get(config.type, Collections.<String>emptyList(), ConfigParserUtil::splitCommaSeparatedString)
          .stream()
          .filter(Predicate.not(String::isBlank))
          .forEach(t -> allowedTypes.add(AllowedType.forName(t).getRecordType()));
    } else {
      allowedTypes = defaults.getAllowedTypes();
    }

    return new RecordConfig(allowedTypes, topic);
  }
}
