package io.zeebe.exporter.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExportFuture implements Future<Long> {
  private final Future<RecordMetadata> wrappedFuture;
  private final long position;

  public ExportFuture(long position, Future<RecordMetadata> wrappedFuture) {
    this.position = position;
    this.wrappedFuture = wrappedFuture;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return wrappedFuture.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return wrappedFuture.isCancelled();
  }

  @Override
  public boolean isDone() {
    return wrappedFuture.isDone();
  }

  @Override
  public Long get() throws InterruptedException, ExecutionException {
    wrappedFuture.get();
    return position;
  }

  @Override
  public Long get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    wrappedFuture.get(timeout, unit);
    return position;
  }
}
