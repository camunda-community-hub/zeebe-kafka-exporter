package io.zeebe.exporter.kafka;

import io.zeebe.exporter.record.Record;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.slf4j.Logger;

public class ZeebeProducer extends KafkaProducer<Record, Record> {
  private final Logger logger;
  private int recordsSinceLastCommit = 0;

  private boolean inTransaction;

  public ZeebeProducer(Configuration configuration, Logger logger) {
    super(new ZeebeProperties(configuration));
    this.logger = logger;
    beginTransaction();
  }

  public void produce(Record record) {
    final ProducerRecord<Record, Record> producerRecord =
        new ProducerRecord<>("topic", record, record);
    send(producerRecord);

    recordsSinceLastCommit++;
    if (recordsSinceLastCommit >= 1000) {
      commitTransaction();

    }
  }

  @Override
  public void close() {
    if (inTransaction) {
      abortTransaction();
    }

    super.close();
  }

  @Override
  public void flush() {
    super.flush();
  }

  private boolean newTransaction(boolean commit) {
    try {
      if (commit) {
        commitTransaction();
      }

      beginTransaction();
    } catch (ProducerFencedException e) {
      logger.error(
          "Another producer with the same transaction ID is currently running; "
              + "this is an unrecoverable error and implies another instance of this exporter is running...",
          e);
      close();
      return false;
    }

    return true;
  }
}
