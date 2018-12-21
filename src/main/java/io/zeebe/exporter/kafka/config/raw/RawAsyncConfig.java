/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package io.zeebe.exporter.kafka.config.raw;

import io.zeebe.exporter.kafka.config.AsyncConfig;
import io.zeebe.util.DurationUtil;

public class RawAsyncConfig {
  public int maxInFlightRecords = 1_000;
  public String awaitInFlightRecordTimeout;

  public AsyncConfig parse() {
    final AsyncConfig parsed = new AsyncConfig();
    parsed.maxInFlightRecords = maxInFlightRecords;

    if (awaitInFlightRecordTimeout != null) {
      parsed.awaitInFlightRecordTimeout = DurationUtil.parse(awaitInFlightRecordTimeout);
    }

    return parsed;
  }
}
