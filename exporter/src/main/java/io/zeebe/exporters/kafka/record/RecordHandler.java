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

import io.zeebe.exporters.kafka.config.RecordConfig;
import io.zeebe.exporters.kafka.config.RecordsConfig;
import io.zeebe.protocol.record.Record;
import org.apache.kafka.clients.producer.ProducerRecord;

public class RecordHandler {
  private final RecordsConfig configuration;

  public RecordHandler(RecordsConfig configuration) {
    this.configuration = configuration;
  }

  public ProducerRecord<Record, Record> transform(Record record) {
    final RecordConfig config = getRecordConfig(record);
    return new ProducerRecord<>(config.getTopic(), record, record);
  }

  public boolean test(Record record) {
    final RecordConfig config = getRecordConfig(record);
    return config.getAllowedTypes().contains(record.getRecordType());
  }

  private <T extends Record> RecordConfig getRecordConfig(T record) {
    return configuration.forType(record.getValueType());
  }
}
