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

import java.util.Optional;
import java.util.function.Function;

final class ConfigParserUtil {
  private ConfigParserUtil() {}

  static <T> T get(T property, T fallback) {
    return Optional.ofNullable(property).orElse(fallback);
  }

  static <T, R> R get(T property, R fallback, Function<T, R> transformer) {
    return Optional.ofNullable(property).map(transformer).orElse(fallback);
  }
}
