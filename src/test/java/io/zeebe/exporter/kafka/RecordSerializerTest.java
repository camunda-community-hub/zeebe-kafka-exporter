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
package io.zeebe.exporter.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.record.RecordMetadata;
import org.junit.Test;

public class RecordSerializerTest {

  private RecordSerializer serializer = new RecordSerializer();

  @Test
  public void shouldSerializeRecordKey() {
    // given
    final Record record = mockRecord(2, 500L, "foo");

    // when
    serializer.configure(null, true);

    // then
    assertThat(serializer.serialize("topic", record)).isEqualTo("2-500".getBytes());
  }

  @Test
  public void shouldSerializeRecord() {
    // given
    final Record record = mockRecord(2, 500L, "foo");

    // when
    serializer.configure(null, false);

    // then
    assertThat(serializer.serialize("topic", record)).isEqualTo("foo".getBytes());
  }

  private Record mockRecord(int partitionId, long position, String json) {
    final Record record = mock(Record.class);
    final RecordMetadata metadata = mock(RecordMetadata.class);
    when(record.getPosition()).thenReturn(position);
    when(record.getMetadata()).thenReturn(metadata);
    when(metadata.getPartitionId()).thenReturn(partitionId);
    when(record.toJson()).thenReturn(json);

    return record;
  }
}
