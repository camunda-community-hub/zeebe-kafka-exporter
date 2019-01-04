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
package io.zeebe.exporter.kafka.record;

import io.zeebe.exporter.kafka.config.RecordConfig;
import io.zeebe.exporter.kafka.config.RecordsConfig;
import io.zeebe.exporter.record.Record;
import org.apache.kafka.clients.producer.ProducerRecord;

public class RecordHandler
    implements RecordTester, RecordTransformer<ProducerRecord<Record, Record>> {
  private final RecordsConfig configuration;

  public RecordHandler(RecordsConfig configuration) {
    this.configuration = configuration;
  }

  @Override
  public ProducerRecord<Record, Record> transform(Record record) {
    final RecordConfig config = getRecordConfig(record);
    return new ProducerRecord<>(config.topic, record, record);
  }

  @Override
  public boolean test(Record record) {
    final RecordConfig config = getRecordConfig(record);
    return config.allowedTypes.contains(record.getMetadata().getRecordType());
  }

  private <T extends Record> RecordConfig getRecordConfig(T record) {
    return configuration.forType(record.getMetadata().getValueType());
  }
}
