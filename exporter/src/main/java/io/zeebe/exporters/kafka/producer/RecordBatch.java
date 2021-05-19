package io.zeebe.exporters.kafka.producer;

import io.zeebe.exporters.kafka.record.FullRecordBatchException;
import io.zeebe.exporters.kafka.serde.RecordId;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Represents a batch of producer records which can be committed at will. Implementations can decide
 * whether to bound the batch, or the semantics of it, as long as they respect this contract.
 *
 * <p>NOTE: while it may seem like overhead to create this abstraction, it gives us the following:
 *
 * <ul>
 *   <li>Separation of concerns allowing us to test the Kafka Producer specific code in a more
 *       narrow setting, making it easier to unit test
 *   <li>Easily swap out the default transactional behavior later on for a non transactional one if
 *       there are major downsides with transactions (as we still need to deal with at least once
 *       anyway due to Zeebe)
 * </ul>
 */
public interface RecordBatch extends AutoCloseable {

  /**
   * Adds the record to the batch. May throw {@link FullRecordBatchException} if the batch is
   * bounded. Unbounded implementations are free to erase the throws portion of the signature.
   *
   * @param record the record to add
   * @throws FullRecordBatchException if the batch is full
   */
  void add(final ProducerRecord<RecordId, byte[]> record) throws FullRecordBatchException;

  /**
   * Commits the batch, returning the highest guaranteed exported position. This is expected to be a
   * blocking operation - if it returns with a value, it should then be guaranteed that ALL records
   * up to that position have been committed. On success, the batch should be cleared and new
   * records can be added to it.
   *
   * <p>NOTE: This method should not throw any error, as it's not expected to be called from a path
   * where errors can be safely handled, i.e. in a scheduled task.
   *
   * <p>NOTE: this is expected to be an atomic operation. Either ALL records were flushed, or none
   * of them were.
   */
  void flush();

  /**
   * Should release any resources belonging to the batch. It's not expected that other operations
   * are called after this.
   */
  void close();
}
