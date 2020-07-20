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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.error.ShouldNotBeNull;

/**
 * Utility assertions to compare JSON values even when they are strings. This uses {@link
 * JsonNode#equals(Object)} under the hood.
 */
@SuppressWarnings("UnusedReturnValue")
public class JsonAssert extends AbstractStringAssert<JsonAssert> {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JsonNode jsonTree;

  /**
   * @param json the actual JSON value
   * @throws AssertionError if {@code json} is not valid JSON
   */
  public JsonAssert(final @NonNull String json) {
    super(json, JsonAssert.class);

    try {
      jsonTree = MAPPER.readTree(json);
    } catch (final JsonProcessingException e) {
      throwAssertionError(new ShouldBeJson(json, e.getMessage()));
    }
  }

  /**
   * Tests if the given JSON string is logically equivalent to the {@code actual} value.
   *
   * @param other the JSON string to test against
   * @return true if both JSON are logically equivalent
   * @throws AssertionError if {@code other} is not valid JSON
   * @throws AssertionError if {@code other} is not JSON-equals to {@code actual}
   * @throws AssertionError if {@code other} is null
   */
  public JsonAssert isJsonEqualTo(final @Nullable String other) {
    final JsonNode otherTree;

    if (other == null) {
      throwAssertionError(ShouldNotBeNull.shouldNotBeNull(other));
      return myself;
    }

    try {
      otherTree = MAPPER.readTree(other);
    } catch (final JsonProcessingException e) {
      throwAssertionError(new ShouldBeJson(other, e.getMessage()));
      return myself; // throws before this is reached
    }

    Assertions.assertThat(jsonTree).isEqualTo(otherTree);
    return myself;
  }

  private static final class ShouldBeJson extends BasicErrorMessageFactory {
    private ShouldBeJson(final String actual, final String errorMessage) {
      super("%nExpecting:%n <%s>%nto be valid JSON, but got: <%s>", actual, errorMessage);
    }
  }
}
