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
package io.zeebe.exporters.kafka.qa;

import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.protocol.record.Record;
import io.zeebe.protocol.record.RecordValue;

final class ConsumedRecord<T extends RecordValue> {
  private final RecordId id;
  private final Record<T> record;

  public ConsumedRecord(final RecordId id, final Record<T> record) {
    this.id = id;
    this.record = record;
  }

  public RecordId getId() {
    return id;
  }

  public Record<T> getRecord() {
    return record;
  }
}
