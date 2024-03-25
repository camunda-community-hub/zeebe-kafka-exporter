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
package at.phactum.zeebe.exporters.kafka.config.raw;

@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public final class RawConfig {
  /**
   * Controls the number of records to buffer in a single record batch before forcing a flush. Note
   * that a flush may occur before anyway due to periodic flushing. This setting should help you
   * estimate a soft upper bound to the memory consumption of the exporter. If you assume a worst
   * case scenario where every record is the size of your zeebe.broker.network.maxMessageSize, then
   * the memory required by the exporter would be at least: (maxBatchSize *
   * zeebe.broker.network.maxMessageSize * 2)
   *
   * <p>We multiply by 2 as the records are buffered twice - once in the exporter itself, and once
   * in the producer's network buffers (but serialized at that point). There's some additional
   * memory overhead used by the producer as well for compression/encryption/etc., so you have to
   * add a bit, but that one is not proportional to the number of records and is more or less
   * constant.
   *
   * <p>Once the batch has reached this size, a flush is automatically triggered. Too small a number
   * here would cause many flush, which is not good for performance, but would mean you will see
   * your records faster/sooner.
   */
  public Integer maxBatchSize;

  /**
   * How often should the current batch be flushed to Kafka, regardless of whether its full or not.
   */
  public Long flushIntervalMs;

  /** Producer specific configuration; see {@link RawProducerConfig}. */
  public RawProducerConfig producer;

  /** Records specific configuration; see {@link RawRecordsConfig}. */
  public RawRecordsConfig records;
}
