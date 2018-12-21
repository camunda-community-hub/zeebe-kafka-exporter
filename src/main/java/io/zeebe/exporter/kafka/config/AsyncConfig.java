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
package io.zeebe.exporter.kafka.config;

import io.zeebe.exporter.kafka.config.raw.RawAsyncConfig;
import io.zeebe.util.DurationUtil;
import java.time.Duration;

public class AsyncConfig {
  public static final Duration DEFAULT_AWAIT_IN_FLIGHT_RECORD_TIMEOUT = Duration.ofSeconds(5);
  public static final int DEFAULT_MAX_IN_FLIGHT_RECORDS = 1_000;

  public int maxInFlightRecords = DEFAULT_MAX_IN_FLIGHT_RECORDS;
  public Duration awaitInFlightRecordTimeout = DEFAULT_AWAIT_IN_FLIGHT_RECORD_TIMEOUT;

  public void parse(RawAsyncConfig raw) {
    this.maxInFlightRecords = raw.maxInFlightRecords;

    if (raw.awaitInFlightRecordTimeout != null) {
      this.awaitInFlightRecordTimeout = DurationUtil.parse(raw.awaitInFlightRecordTimeout);
    }
  }
}
