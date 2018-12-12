package io.zeebe.exporter.kafka;

import io.zeebe.exporter.context.Context;
import io.zeebe.exporter.context.Controller;
import io.zeebe.exporter.record.Record;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.OutOfOrderSequenceException;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.slf4j.Logger;

public class Exporter implements io.zeebe.exporter.spi.Exporter {
  private static final int UNSET_POSITION = -1;

  private Controller controller;
  private Configuration configuration;
  private Logger logger;

  private ProducerRecord<Record, Record> producerRecord;
  private ZeebeProducer producer;
  private long latestRecordPosition = UNSET_POSITION;
  private int recordsSinceLastFlush = 0;

  @Override
  public void configure(Context context) {
    this.logger = context.getLogger();
    this.configuration = context.getConfiguration().instantiate(Configuration.class);
  }

  @Override
  public void open(Controller controller) {
    this.controller = controller;
    this.producer = new ZeebeProducer(this.configuration, this.logger);


  }

  @Override
  public void close() {
    if (producer != null) {
      flush(false);
      producer.close();
      producer = null;
    }
  }

  @Override
  public void export(Record record) {
    // The producer may be closed prematurely if an unrecoverable exception occurred, at which point
    // we ignore any further records; this way we do not block the exporter processor, and on
    // restart will reprocess all other records that we "missed" here.
    if (producer == null) {
      return;
    }

    final ProducerRecord<Record, Record> producedRecord =
        new ProducerRecord<>("topic", record, record);
    producer.send(producedRecord);
    latestRecordPosition = record.getPosition();
    recordsSinceLastFlush++;

    if (recordsSinceLastFlush >= 1000) {
      flush(true);
    }
  }

  private void flush(boolean beginTransaction) {
    if (latestRecordPosition == UNSET_POSITION) {
      return;
    }

    try {
      producer.commitTransaction();
      controller.updateLastExportedRecordPosition(latestRecordPosition);
      latestRecordPosition = -1;
      recordsSinceLastFlush = 0;
      if (beginTransaction) {
        producer.beginTransaction();
      }
    } catch (IllegalStateException e) {
      onUnrecoverableError("the Zeebe Kafka producer is misconfigured for transactions", e);
    } catch (ProducerFencedException e) {
      onUnrecoverableError(
          "another producer is currently running and configured with the same transaction ID", e);
    } catch (OutOfOrderSequenceException e) {
      onUnrecoverableError(
          "Kafka received an unexpected sequence number from the Zeebe producer, which means that data may have been lost",
          e);
    } catch (AuthorizationException e) {
      onUnrecoverableError(
          "the Zeebe producer is misconfigured and not authorized for the given Kafka broker", e);
    } catch (KafkaException e) {
      producer.abortTransaction();
    }
  }

  private void onUnrecoverableError(String details, Exception e) {
    final String message =
        String.format(
            "Unrecoverable error occurred: %s; closing producer, all subsequent records will be ignored.",
            details);
    logger.error(message, e);
    close();
  }
}
