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
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Keeps track of all in-flight {@link Request}, in order of which they are sent (which correlates
 * with the order of their {@link Request#position}. This queue has a maximum size, and it's
 * recommended not to set it too high to avoid consumming too much memory.
 */
public final class RequestQueue {
  private final Queue<Request> requests;
  private final int maxSize;

  /** @param maxSize the maximum number of requests to keep in memory */
  public RequestQueue(final int maxSize) {
    this.maxSize = maxSize;
    this.requests = new ArrayDeque<>(maxSize);
  }

  /**
   * Cancels all in-flight requests and stops tracking them. Note that this may not cancel requests
   * if they were already sent.
   */
  public void cancelAll() {
    requests.forEach(r -> r.cancel(true));
    requests.clear();
  }

  /**
   * Appends a new request to the queue if there is space available.
   *
   * @param request the request to add
   * @return true if appended, false if the queue is full
   */
  public boolean offer(final @NonNull Request request) {
    Objects.requireNonNull(request);

    if (requests.size() == maxSize) {
      return false;
    }

    return requests.add(request);
  }

  /**
   * Consumes all requests which are {@link Request#isDone()}, passing them in order to the consumer
   * and removing them from the queue. While this is not thread safe, we assume the exporter runs in
   * a single thread.
   *
   * @param consumer the consumer which accepts completed requests
   */
  public void consumeCompleted(final @NonNull Consumer<Request> consumer) {
    Objects.requireNonNull(consumer);
    final Iterator<Request> iterator = requests.iterator();

    while (iterator.hasNext()) {
      final Request request = iterator.next();
      if (!request.isDone()) {
        break;
      }

      consumer.accept(request);
      iterator.remove();
    }
  }

  /**
   * Peeks at the oldest request in the queue and passes it on to the consumer, removing it once the
   * consumer is done with it. Does nothing if there are no requests in the queue. This is used
   * primarily to block until the oldest request is finished.
   *
   * @param consumer the consumer which may receive a request
   */
  public void consume(final @NonNull Consumer<Request> consumer) {
    Objects.requireNonNull(consumer);
    final Request request = requests.peek();

    if (request != null) {
      consumer.accept(request);
      requests.remove();
    }
  }
}
