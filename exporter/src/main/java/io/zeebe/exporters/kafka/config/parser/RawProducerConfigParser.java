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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.zeebe.exporters.kafka.config.ProducerConfig;
import io.zeebe.exporters.kafka.config.raw.RawProducerConfig;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * {@link RawProducerConfigParser} parses instances of {@link RawProducerConfig} into valid
 * instances of {@link ProducerConfig}, substituting sane defaults for missing properties.
 *
 * <p>One thing to note, is it will parse the {@link RawProducerConfig#config} string as if it were
 * a properties file, delegating this to {@link Properties#load(Reader)}.
 */
public class RawProducerConfigParser implements ConfigParser<RawProducerConfig, ProducerConfig> {
  static final List<String> DEFAULT_SERVERS = Collections.singletonList("localhost:9092");
  static final String DEFAULT_CLIENT_ID = "zeebe";
  static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(20);
  static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(5);
  static final String DEFAULT_FORMAT = "json";

  @Override
  public @NonNull ProducerConfig parse(final @Nullable RawProducerConfig config) {
    Objects.requireNonNull(config);

    final List<String> servers =
        get(config.servers, DEFAULT_SERVERS, ConfigParserUtil::splitCommaSeparatedString);
    final String clientId = get(config.clientId, DEFAULT_CLIENT_ID);
    final Duration closeTimeout =
        get(config.closeTimeoutMs, DEFAULT_CLOSE_TIMEOUT, Duration::ofMillis);
    final Duration requestTimeout =
        get(config.requestTimeoutMs, DEFAULT_REQUEST_TIMEOUT, Duration::ofMillis);
    final Map<String, Object> producerConfig =
        get(config.config, new HashMap<>(), this::parseProperties);
    final String format = get(config.format, DEFAULT_FORMAT);

    return new ProducerConfig(
        clientId, closeTimeout, producerConfig, requestTimeout, servers, format);
  }

  private @NonNull Map<String, Object> parseProperties(final @NonNull String propertiesString) {
    final Properties properties = new Properties();
    final Map<String, Object> parsed = new HashMap<>();

    try {
      properties.load(new StringReader(propertiesString));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

    for (final String property : properties.stringPropertyNames()) {
      parsed.put(property, properties.get(property));
    }

    return parsed;
  }
}
