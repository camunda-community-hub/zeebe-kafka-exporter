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
package io.zeebe.exporters.kafka;

import io.zeebe.exporters.kafka.batch.BatchedRecord;
import java.time.Duration;

public final class BlockingRequestTimeoutException extends RuntimeException {

  private static final long serialVersionUID = 8852392241516861883L;
  private final BatchedRecord record;
  private final Duration timeout;

  public BlockingRequestTimeoutException(
      final BatchedRecord record, final Duration timeout, final Throwable cause) {
    super(
        String.format(
            "Timed out after %s awaiting record %s to be acknowledged by Kafka", timeout, record),
        cause);

    this.record = record;
    this.timeout = timeout;
  }

  public BatchedRecord getRecord() {
    return record;
  }

  public Duration getTimeout() {
    return timeout;
  }
}
