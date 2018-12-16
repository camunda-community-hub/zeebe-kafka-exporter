package io.zeebe.exporter.kafka;

import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.record.RecordMetadata;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
