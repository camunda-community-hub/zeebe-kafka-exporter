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
package io.zeebe.exporters.kafka.record;

@SuppressWarnings("unused")
public final class FullRecordBatchException extends RuntimeException {
  private static final String MESSAGE_FORMAT =
      "No new records can be added to the record batch with a maximum size of %d";

  private final int maxBatchSize;

  public FullRecordBatchException(final int maxBatchSize, final Throwable cause) {
    super(String.format(MESSAGE_FORMAT, maxBatchSize), cause);
    this.maxBatchSize = maxBatchSize;
  }

  public int getMaxBatchSize() {
    return maxBatchSize;
  }
}
