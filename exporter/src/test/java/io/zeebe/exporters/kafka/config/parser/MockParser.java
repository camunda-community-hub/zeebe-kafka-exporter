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

public class MockParser<T, R> implements Parser<T, R> {
  private final Parser<T, R> defaultParser;
  public R config;

  public MockParser(Parser<T, R> defaultParser) {
    this.defaultParser = defaultParser;
  }

  @Override
  public R parse(T config) {
    if (this.config == null) {
      return defaultParser.parse(config);
    }

    return this.config;
  }
}
