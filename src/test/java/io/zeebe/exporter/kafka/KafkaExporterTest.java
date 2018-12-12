package io.zeebe.exporter.kafka;

import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.record.RecordMetadata;
import io.zeebe.exporter.test.Configuration;
import io.zeebe.exporter.test.Context;
import io.zeebe.exporter.test.Controller;
import io.zeebe.exporter.test.ScheduledTask;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;
import org.apache.kafka.clients.producer.MockProducer;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class KafkaExporterTest {

  private final Context exporterContext = new Context();
  private final Configuration exporterConfig = new Configuration();
  private final Controller exporterController = new Controller();
  private final Logger logger = LoggerFactory.getLogger(KafkaExporter.class);

  private final KafkaExporter exporter = new KafkaExporter();
  private final KafkaExporterConfiguration configuration = spy(new KafkaExporterConfiguration());
  private final MockProducer<Record, Record> mockProducer = new MockProducer<>();

  @Before
  public void setup() {
    configuration.setTopic("topic");
    exporterConfig.setId("kafka");
    exporterConfig.setConfiguration(configuration);
    doAnswer(i -> mockProducer).when(configuration).newProducer();

    exporterContext.setConfiguration(exporterConfig);
    exporterContext.setLogger(logger);
  }

  @Test
  public void shouldExportRecords() {
    // given
    final long position = 2L;
    final Record record = mockRecord(ValueType.MESSAGE, RecordType.COMMAND);
    when(record.getPosition()).thenReturn(position);

    // when
    exporter.configure(exporterContext);
    exporter.open(exporterController);
    exporter.export(record);

    // then
    mockProducer.completeNext();
    final ScheduledTask task = exporterController.getScheduledTasks().get(0);
    task.getTask().run();
    assertThat(exporterController.getPosition()).isEqualTo(position);
  }

  private Record mockRecord(final ValueType valueType, final RecordType recordType) {
    final RecordMetadata metadata = mock(RecordMetadata.class);
    when(metadata.getValueType()).thenReturn(valueType);
    when(metadata.getRecordType()).thenReturn(recordType);

    final Record record = mock(Record.class);
    when(record.getMetadata()).thenReturn(metadata);

    return record;
  }
}
