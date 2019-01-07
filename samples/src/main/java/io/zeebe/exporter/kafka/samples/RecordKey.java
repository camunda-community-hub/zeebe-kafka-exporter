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
package io.zeebe.exporter.kafka.samples;

public class RecordKey {
  private final int partitionId;
  private final long position;

  RecordKey(int partitionId, long position) {
    this.partitionId = partitionId;
    this.position = position;
  }

  public int getPartitionId() {
    return partitionId;
  }

  public long getPosition() {
    return position;
  }
}
