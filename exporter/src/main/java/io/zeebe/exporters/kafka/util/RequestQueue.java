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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;

public class RequestQueue {
  private final Queue<Request> requests;
  private final int maxSize;

  public RequestQueue(int maxSize) {
    this.maxSize = maxSize;
    this.requests = new ArrayDeque<>(maxSize);
  }

  public void cancelAll() {
    requests.forEach(r -> r.cancel(true));
    requests.clear();
  }

  public boolean offer(Request request) {
    if (requests.size() == maxSize) {
      return false;
    }

    return requests.add(request);
  }

  public void consumeCompleted(Consumer<Request> consumer) {
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

  public void consume(Consumer<Request> consumer) {
    Objects.requireNonNull(consumer);
    final Request request = requests.peek();

    if (request != null) {
      consumer.accept(request);
      requests.remove();
    }
  }
}
