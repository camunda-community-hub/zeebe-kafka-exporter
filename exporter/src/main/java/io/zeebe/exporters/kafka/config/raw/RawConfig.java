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
package io.zeebe.exporters.kafka.config.raw;

@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class RawConfig {
  /**
   * Controls how many records can have been sent to the Kafka broker without any acknowledgment.
   * Once the limit is reached the exporter will block and wait until either one record is
   * acknowledged
   */
  public Integer maxInFlightRecords;

  /**
   * How often should the exporter drain the in flight records' queue of completed requests and
   * update the broker with the guaranteed latest exported position
   */
  public Long inFlightRecordCheckIntervalMs;

  /** Producer specific configuration; see {@link RawProducerConfig}. */
  public RawProducerConfig producer;

  /** Records specific configuration; see {@link RawRecordsConfig}. */
  public RawRecordsConfig records;
}
