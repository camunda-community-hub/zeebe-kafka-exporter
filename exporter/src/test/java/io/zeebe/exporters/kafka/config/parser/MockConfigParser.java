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

import java.util.Objects;

/**
 * {@link MockConfigParser} allows setting a predefined parsed value for any given value. If not
 * set, it will delegate to an underlying parser of the same types, and memoize the value, such that
 * every subsequent {@link #parse(Object)} call will return the same object.
 *
 * <p>You can override this by calling {@link #forceParse(Object)} if you need.
 *
 * @param <T> {@inheritDoc}
 * @param <R> {@inheritDoc}
 */
public final class MockConfigParser<T, R> implements ConfigParser<T, R> {
  public R config;

  private final ConfigParser<T, R> delegate;

  public MockConfigParser(final ConfigParser<T, R> delegate) {
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public R parse(final T config) {
    if (this.config == null) {
      this.config = delegate.parse(config);
    }

    return this.config;
  }

  /** A helper method in tests to force re-parsing an updated configuration. */
  public void forceParse(final T config) {
    this.config = null;
    parse(config);
  }
}
