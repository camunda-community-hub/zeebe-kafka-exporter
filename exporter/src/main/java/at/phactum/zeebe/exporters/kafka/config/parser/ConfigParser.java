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
package at.phactum.zeebe.exporters.kafka.config.parser;

import java.util.function.Supplier;

/**
 * {@link ConfigParser} is a single-responsibility interface which should parse any given instance
 * of type {@code T} into a valid instance of type {@code R}.
 *
 * @param <T> the raw configuration type to be parsed
 * @param <R> the parsed configuration type
 */
@FunctionalInterface
public interface ConfigParser<T, R> {

  R parse(T config);

  default R parse(T config, final Supplier<T> defaultValue) {
    if (config == null) {
      config = defaultValue.get();
    }

    return parse(config);
  }
}
