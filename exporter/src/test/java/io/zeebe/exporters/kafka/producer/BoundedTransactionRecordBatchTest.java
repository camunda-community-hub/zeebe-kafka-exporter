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
package io.zeebe.exporters.kafka.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zeebe.exporters.kafka.config.parser.RawProducerConfigParser;
import io.zeebe.exporters.kafka.config.raw.RawProducerConfig;
import io.zeebe.exporters.kafka.record.FullRecordBatchException;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.serde.RecordIdSerializer;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.agrona.collections.MutableLong;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
@Execution(ExecutionMode.CONCURRENT)
final class BoundedTransactionRecordBatchTest {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BoundedTransactionRecordBatchTest.class);

  private final MockKafkaProducerFactory mockProducerFactory =
      new MockKafkaProducerFactory(this::newMockProducer);

  @Test
  void shouldNotClearRecordsOnClose() {
    // given
    final var flushedPosition = new MutableLong(0L);
    final var batch = createBatch(flushedPosition::set, 1);
    batch.add(new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]));

    // when
    batch.close();
    batch.flush();

    // then
    assertThat(flushedPosition.get())
        .as("the record added before close should have been flushed")
        .isEqualTo(1L);
  }

  @Test
  void shouldCloseProducerOnClose() {
    // given
    final var batch = createBatch(position -> {}, 1);

    // enforces creating the producer
    batch.add(new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]));
    assertThat(mockProducerFactory.mockProducer)
        .as("a producer should have been created when adding a new record")
        .isNotNull();

    // when
    batch.close();

    // then
    assertThat(mockProducerFactory.mockProducer.closed())
        .as("the producer should have been closed when closing the batch")
        .isTrue();
  }

  @Test
  void shouldCloseSafelyEvenIfNothingInitialized() {
    // given
    final var batch = createBatch(position -> {}, 1);

    // then
    assertThatCode(batch::close).doesNotThrowAnyException();
  }

  @Test
  void shouldSendUnsentRecordsOnFlush() {
    // given
    final var records =
        List.of(
            new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]),
            new ProducerRecord<>("zeebe", new RecordId(1, 2), new byte[0]));
    final var batch = createBatch(position -> {}, records.size());
    batch.add(records.get(0));
    batch.add(records.get(1));

    // when - closing will reset the producer, making any previously added records "unsent"
    batch.close();
    batch.flush();

    // then
    assertThat(mockProducerFactory.mockProducer.history())
        .as("all records from the batch should have been sent via the producer")
        .containsExactlyElementsOf(records);
  }

  @Test
  void shouldClearBatchAfterFlush() {
    // given
    final var flushedPosition = new MutableLong(0L);
    final var records =
        List.of(
            new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]),
            new ProducerRecord<>("zeebe", new RecordId(1, 2), new byte[0]));
    final var batch = createBatch(flushedPosition::set, 1);

    // when - flushing twice, we can check if there were duplicates, which there would be if we
    // didn't clear the batch after a successful flush
    batch.add(records.get(0));
    batch.flush();
    batch.add(records.get(1));
    batch.flush();

    // then
    assertThat(mockProducerFactory.mockProducer.history())
        .as("there should be no duplicates sent via the producer")
        .containsExactlyElementsOf(records);
  }

  @Test
  void shouldCommitTransactionOnFlush() {
    // given
    final var records =
        List.of(
            new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]),
            new ProducerRecord<>("zeebe", new RecordId(1, 2), new byte[0]));
    final var batch = createBatch(position -> {}, 2);

    // when
    batch.add(records.get(0));
    batch.add(records.get(1));
    batch.flush();

    // then
    assertThat(mockProducerFactory.mockProducer.transactionCommitted())
        .as("the transaction should have been committed")
        .isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("recoverableErrorProvider")
  void shouldNotResetProducerOnRecoverableErrorDuringFlush(
      final String caseName, final Consumer<MockProducer<RecordId, byte[]>> failureMode) {
    // given
    final var position = new MutableLong(0L);
    final var records =
        List.of(
            new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]),
            new ProducerRecord<>("zeebe", new RecordId(1, 2), new byte[0]));
    final var batch = createBatch(position::set, 2);
    batch.add(records.get(0));
    batch.add(records.get(1));
    batch.close();

    // when - fix the mock producer to a controllable one, then on temporary failure, we can check
    // that it's not closed, and that flushing again will not produce duplicates
    mockProducerFactory.mockProducer = newMockProducer();
    final var failingProducer = mockProducerFactory.mockProducer;
    failureMode.accept(failingProducer);
    batch.flush();
    resetFailureMode(failingProducer);
    batch.flush();

    // then
    assertThat(failingProducer.closed())
        .as("the producer should not have been closed on recoverable error")
        .isFalse();
    assertThat(failingProducer.history())
        .as("there should be no duplicates sent via the producer")
        .containsExactlyElementsOf(records);
    assertThat(position.get()).as("should have flushed the correct new position").isEqualTo(2L);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unrecoverableErrorProvider")
  void shouldResetProducerOnUnrecoverableError(
      final String caseName, final Consumer<MockProducer<RecordId, byte[]>> failureMode) {
    // given
    final var position = new MutableLong(0L);
    final var records =
        List.of(
            new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]),
            new ProducerRecord<>("zeebe", new RecordId(1, 2), new byte[0]));
    final var batch = createBatch(position::set, 2);
    batch.add(records.get(0));
    batch.add(records.get(1));
    batch.close();

    // when - close the batch first so we can test errors the whole transaction lifecycle
    mockProducerFactory.mockProducer = newMockProducer();
    final var failingProducer = mockProducerFactory.mockProducer;

    failureMode.accept(failingProducer);
    batch.flush();
    batch.flush();

    // then
    assertThat(failingProducer.closed()).as("the producer should have been closed").isTrue();
    assertThat(mockProducerFactory.mockProducer)
        .as("a new producer should have been created")
        .isNotSameAs(failingProducer);
    assertThat(mockProducerFactory.mockProducer.history())
        .as("the records should have been sent via a new producer")
        .containsExactlyElementsOf(records);
    assertThat(position.get()).as("should have flushed the correct new position").isEqualTo(2L);
  }

  @Test
  void shouldSendRecordImmediatelyWhenAdded() {
    // given
    final var position = new MutableLong(0L);
    final var record = new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]);
    final var batch = createBatch(position::set, 2);

    // when
    batch.add(record);

    // then
    assertThat(mockProducerFactory.mockProducer.transactionInitialized())
        .as("the producer was correctly initialized")
        .isTrue();
    assertThat(mockProducerFactory.mockProducer.transactionInFlight())
        .as("records are sent within a transaction")
        .isTrue();
    assertThat(mockProducerFactory.mockProducer.uncommittedRecords())
        .as("the record should have been sent")
        .containsExactly(record);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unrecoverableErrorProvider")
  void shouldReuseInFlightTransactionOnAdd() {
    // given
    final var position = new MutableLong(0L);
    final var record = new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]);
    final var batch = createBatch(position::set, 2);

    // when
    batch.add(record);
    batch.add(record);

    // then
    assertThat(mockProducerFactory.mockProducer.transactionInFlight())
        .as("records are sent within a transaction")
        .isTrue();
    assertThat(mockProducerFactory.mockProducer.commitCount())
        .as("no transaction was committed")
        .isZero();
  }

  @Test
  void shouldFlushBatchOnAddIfFull() {
    // given
    final var position = new MutableLong(0L);
    final var records =
        List.of(
            new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]),
            new ProducerRecord<>("zeebe", new RecordId(1, 2), new byte[0]));
    final var batch = createBatch(position::set, 1);
    batch.add(records.get(0));

    // when
    batch.add(records.get(1));

    // then
    assertThat(mockProducerFactory.mockProducer.history())
        .as("the first record should have been flushed")
        .containsExactly(records.get(0));
    assertThat(mockProducerFactory.mockProducer.uncommittedRecords())
        .as("the second record should still be pending")
        .containsExactly(records.get(1));
    assertThat(position.get()).as("should have flushed the correct new position").isEqualTo(1L);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unrecoverableErrorProvider")
  void shouldResetProducerOnErrorWhenFlushingDueToFullBatch(
      final String caseName, final Consumer<MockProducer<RecordId, byte[]>> failureMode) {
    // given
    final var position = new MutableLong(0L);
    final var records =
        List.of(
            new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]),
            new ProducerRecord<>("zeebe", new RecordId(1, 2), new byte[0]));
    final var batch = createBatch(position::set, 1);
    batch.add(records.get(0));
    batch.close();

    // when - close the batch first so we can test errors the whole transaction lifecycle
    mockProducerFactory.mockProducer = newMockProducer();
    final var failingProducer = mockProducerFactory.mockProducer;
    failureMode.accept(failingProducer);
    assertThatThrownBy(() -> batch.add(records.get(1)))
        .isInstanceOf(FullRecordBatchException.class);
    batch.add(records.get(1));

    // then
    assertThat(failingProducer.closed())
        .as("the failing producer should have been closed")
        .isTrue();
    assertThat(mockProducerFactory.mockProducer.history())
        .as("the first record was flushed successfully on the second try")
        .containsExactly(records.get(0));
    assertThat(mockProducerFactory.mockProducer.uncommittedRecords())
        .as("the second record was batched successfully on the second try")
        .containsExactly(records.get(1));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("recoverableErrorProvider")
  void shouldNotResetProducerOnRecoverableErrorOnAddWhenFlushing(
      final String caseName, final Consumer<MockProducer<RecordId, byte[]>> failureMode) {
    // given
    final var position = new MutableLong(0L);
    final var records =
        List.of(
            new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]),
            new ProducerRecord<>("zeebe", new RecordId(1, 2), new byte[0]));
    final var batch = createBatch(position::set, 1);
    batch.add(records.get(0));
    batch.close();

    // when - fix the mock producer to a controllable one, then on temporary failure, we can check
    // that it's not closed, and that flushing again will not produce duplicates
    mockProducerFactory.mockProducer = newMockProducer();
    final var failingProducer = mockProducerFactory.mockProducer;
    failureMode.accept(failingProducer);
    assertThatThrownBy(() -> batch.add(records.get(1)))
        .isInstanceOf(FullRecordBatchException.class);
    resetFailureMode(failingProducer);
    batch.add(records.get(1));

    // then
    assertThat(failingProducer.closed()).as("the producer should not have been closed").isFalse();
    assertThat(failingProducer.history())
        .as("the first record was flushed successfully on the second try")
        .containsExactly(records.get(0));
    assertThat(failingProducer.uncommittedRecords())
        .as("the second record was batched successfully on the second try")
        .containsExactly(records.get(1));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("sendUnrecoverableErrorProvider")
  void shouldResetProducerOnAddOnUnrecoverableSendErrorWithoutReportingIt(
      final String caseName, final Consumer<MockProducer<RecordId, byte[]>> failureMode) {
    // given
    final var position = new MutableLong(0L);
    final var record = new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]);
    final var batch = createBatch(position::set, 1);

    // when - close the batch first so we can test errors the whole transaction lifecycle
    mockProducerFactory.mockProducer = newMockProducer();
    final var failingProducer = mockProducerFactory.mockProducer;
    failureMode.accept(failingProducer);
    batch.add(record);
    batch.flush();

    // then
    assertThat(failingProducer.closed())
        .as("the failing producer should have been closed")
        .isTrue();
    assertThat(mockProducerFactory.mockProducer.history())
        .as("the record should not have been lost")
        .containsExactly(record);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("sendRecoverableErrorProvider")
  void shouldNotResetProducerOnAddOnRecoverableSendError(
      final String caseName, final Consumer<MockProducer<RecordId, byte[]>> failureMode) {
    // given
    final var position = new MutableLong(0L);
    final var record = new ProducerRecord<>("zeebe", new RecordId(1, 1), new byte[0]);
    final var batch = createBatch(position::set, 1);

    // when - close the batch first so we can test errors the whole transaction lifecycle
    mockProducerFactory.mockProducer = newMockProducer();
    final var failingProducer = mockProducerFactory.mockProducer;
    failureMode.accept(failingProducer);
    batch.add(record);
    resetFailureMode(failingProducer);
    batch.flush();

    // then
    assertThat(failingProducer.closed()).as("the producer should not have been closed").isFalse();
    assertThat(failingProducer.history())
        .as("the record should not have been lost")
        .containsExactly(record);
  }

  private BoundedTransactionalRecordBatch createBatch(
      final LongConsumer onFlushCallback, final int maxBatchSize) {
    final var config = new RawProducerConfigParser().parse(new RawProducerConfig());
    return new BoundedTransactionalRecordBatch(
        config, maxBatchSize, onFlushCallback, LOGGER, mockProducerFactory);
  }

  private static Stream<FailureModeCase> recoverableErrorProvider() {
    return Stream.of(new TimeoutException("timed out"), new InterruptException("interrupted"))
        .flatMap(e -> failureModeProvider(() -> e));
  }

  private static Stream<FailureModeCase> unrecoverableErrorProvider() {
    return failureModeProvider(RuntimeException::new);
  }

  private static Stream<FailureModeCase> sendRecoverableErrorProvider() {
    return Stream.of(new TimeoutException("timed out"), new InterruptException("interrupted"))
        .flatMap(e -> sendFailureModeProvider(() -> e));
  }

  private static Stream<FailureModeCase> sendUnrecoverableErrorProvider() {
    return sendFailureModeProvider(RuntimeException::new);
  }

  private static Stream<FailureModeCase> failureModeProvider(
      final Supplier<RuntimeException> exceptionSupplier) {
    return Stream.concat(
        sendFailureModeProvider(exceptionSupplier),
        Stream.of(
            new FailureModeCase(
                "commit transaction error",
                p -> p.commitTransactionException = exceptionSupplier.get())));
  }

  private static Stream<FailureModeCase> sendFailureModeProvider(
      final Supplier<RuntimeException> exceptionSupplier) {
    return Stream.of(
        new FailureModeCase(
            "init transaction error", p -> p.initTransactionException = exceptionSupplier.get()),
        new FailureModeCase(
            "begin transaction error", p -> p.beginTransactionException = exceptionSupplier.get()));
  }

  private MockProducer<RecordId, byte[]> newMockProducer() {
    return new MockProducer<>(true, new RecordIdSerializer(), new ByteArraySerializer());
  }

  private void resetFailureMode(final MockProducer<RecordId, byte[]> producer) {
    producer.beginTransactionException = null;
    producer.initTransactionException = null;
    producer.commitTransactionException = null;
  }

  private static class FailureModeCase implements Arguments {
    private final String name;
    private final Consumer<MockProducer<RecordId, byte[]>> producerConsumer;

    public FailureModeCase(
        final String name, final Consumer<MockProducer<RecordId, byte[]>> producerConsumer) {
      this.name = name;
      this.producerConsumer = producerConsumer;
    }

    @Override
    public Object[] get() {
      return new Object[] {name, producerConsumer};
    }
  }
}
