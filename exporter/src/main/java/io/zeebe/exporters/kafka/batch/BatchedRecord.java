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
package io.zeebe.exporters.kafka.batch;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.zeebe.exporters.kafka.serde.RecordId;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class BatchedRecord {
  private final ProducerRecord<RecordId, byte[]> record;

  private Future<RecordMetadata> request;

  public BatchedRecord(
      final @NonNull ProducerRecord<RecordId, byte[]> record,
      final @NonNull Future<RecordMetadata> request) {
    this.record = Objects.requireNonNull(record);
    this.request = Objects.requireNonNull(request);
  }

  @NonNull
  public ProducerRecord<RecordId, byte[]> getRecord() {
    return record;
  }

  public int size() {
    return record.value().length;
  }

  public void retry(@NonNull final Future<RecordMetadata> request) {
    this.request = Objects.requireNonNull(request);
  }

  public boolean wasExported() {
    return request.isDone() && !wasCancelled();
  }

  public void cancel() {
    request.cancel(true);
  }

  public boolean wasCancelled() {
    return request.isCancelled();
  }

  public void awaitCompletion(final Duration timeout)
      throws InterruptedException, ExecutionException, TimeoutException {
    if (wasExported()) {
      return;
    }

    // throws an exception if the request failed, this is interrupted, or it was not completed
    // before the given timeout
    request.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRecord(), request);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final BatchedRecord other = (BatchedRecord) o;
    return getRecord().equals(other.getRecord()) && request.equals(other.request);
  }

  @Override
  public String toString() {
    return "BatchedRecord{"
        + "recordId="
        + record.key()
        + ", isDone="
        + wasExported()
        + ", size="
        + size()
        + '}';
  }
}
