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
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.common.errors.RetriableException;

public final class BatchedRecordException extends RuntimeException {
  private static final long serialVersionUID = -5912941196852862280L;

  private final transient BatchedRecord record;

  public BatchedRecordException(
      final @NonNull BatchedRecord record, final @NonNull ExecutionException exception) {
    this(record, exception.getCause());
  }

  public BatchedRecordException(
      final @NonNull BatchedRecord record, final @NonNull Throwable cause) {
    super(Objects.requireNonNull(cause));
    this.record = record;
  }

  @NonNull
  public BatchedRecord getRecord() {
    return record;
  }

  public boolean isRecoverable() {
    return getCause() instanceof RetriableException;
  }
}
