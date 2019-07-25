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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.RecordMetadata;

public class Request implements Future<Long> {
  private final Future<RecordMetadata> wrappedFuture;
  private final long position;

  public Request(long position, Future<RecordMetadata> wrappedFuture) {
    this.position = position;
    this.wrappedFuture = wrappedFuture;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
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

  @Override
  public Long get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    wrappedFuture.get(timeout, unit);
    return position;
  }
}
