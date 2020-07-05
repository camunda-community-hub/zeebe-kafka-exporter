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
package io.zeebe.exporters.kafka.util;

import edu.umd.cs.findbugs.annotations.NonNull;

/** Entry point to create {@link JsonAssert}. */
public class JsonAssertions {

  /**
   * @param json the actual JSON to test
   * @return a {@link JsonAssert} which tests values against the given {@code json}
   */
  public static @NonNull JsonAssert assertThat(final @NonNull String json) {
    return new JsonAssert(json);
  }
}
