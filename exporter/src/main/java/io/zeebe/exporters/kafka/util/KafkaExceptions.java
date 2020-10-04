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
package io.zeebe.exporters.kafka.util;

import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.CorruptRecordException;
import org.apache.kafka.common.errors.InvalidMetadataException;
import org.apache.kafka.common.errors.NotEnoughReplicasAfterAppendException;
import org.apache.kafka.common.errors.NotEnoughReplicasException;
import org.apache.kafka.common.errors.OffsetOutOfRangeException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

public final class KafkaExceptions {
  private static final Set<Class<? extends Throwable>> RECOVERABLE_EXCEPTIONS =
      Set.of(
          CorruptRecordException.class,
          InvalidMetadataException.class,
          NotEnoughReplicasAfterAppendException.class,
          NotEnoughReplicasException.class,
          OffsetOutOfRangeException.class,
          TimeoutException.class,
          UnknownTopicOrPartitionException.class);

  private KafkaExceptions() {}

  /**
   * Determines whether or not an exception produced by a {@link
   * org.apache.kafka.clients.producer.KafkaProducer#send(ProducerRecord)} call can be retried or
   * not.
   *
   * <p>See {@link org.apache.kafka.clients.producer.Callback} for more.
   *
   * @param exception the exception to retry
   * @see org.apache.kafka.clients.producer.Callback
   * @return true if the exception can be retried, false otherwise
   */
  public static boolean shouldRetry(final Throwable exception) {
    return RECOVERABLE_EXCEPTIONS.contains(exception.getClass());
  }
}
