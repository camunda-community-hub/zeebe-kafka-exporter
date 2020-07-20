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
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Represents an in-flight request as a future, which when completed returns the position of the
 * record that was successfully acknowledged by Kafka.
 *
 * <p>Note that while we treat it as an individual request, it's most likely batched by the Kafka
 * producer and send in group.
 */
public final class Request implements Future<Long> {
  private final Future<RecordMetadata> wrappedFuture;
  private final long position;

  /**
   * @param position the highest position of any record tracked by this request.
   * @param wrappedFuture the Kafka request's future with which we can track if the records are
   *     acknowledged
   */
  public Request(final long position, final @NonNull Future<RecordMetadata> wrappedFuture) {
    this.position = position;
    this.wrappedFuture = Objects.requireNonNull(wrappedFuture);
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    return wrappedFuture.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return wrappedFuture.isCancelled();
  }

  @Override
  public boolean isDone() {
    return wrappedFuture.isDone();
  }

  @Override
  public Long get() throws InterruptedException, ExecutionException {
    wrappedFuture.get();
    return position;
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public Long get(final long timeout, final TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    wrappedFuture.get(timeout, unit);
    return position;
  }
}
