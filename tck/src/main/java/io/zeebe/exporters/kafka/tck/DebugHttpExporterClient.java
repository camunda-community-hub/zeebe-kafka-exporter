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
package io.zeebe.exporters.kafka.tck;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.zeebe.protocol.immutables.record.ImmutableRecord;
import io.zeebe.protocol.record.Record;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class DebugHttpExporterClient implements RecordStreamer {

  private static final ObjectReader READER =
      new ObjectMapper().readerFor(new TypeReference<List<ImmutableRecord<?>>>() {});

  private final URL serverUrl;

  public DebugHttpExporterClient(final URL serverUrl) {
    this.serverUrl = serverUrl;
  }

  @Override
  public Stream<Record<?>> streamRecords() {
    try {
      // the HTTP exporter returns records in reversed order, so flip them before returning
      final List<ImmutableRecord<?>> records = READER.readValue(serverUrl);
      Collections.reverse(records);

      return records.stream().map(r -> r);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
