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
package io.zeebe.exporters.kafka.serde.generic;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Message;
import io.zeebe.exporters.kafka.serde.util.MockSchemaTransformer;
import io.zeebe.exporters.kafka.serde.util.SchemaFactory;
import io.zeebe.test.exporter.record.MockRecord;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Actual serialization isn't tested as that's performed by the exporter-protobuf module */
@RunWith(Parameterized.class)
public class GenericRecordSerializationTest {
  private final StringSerializer headerSerializer = new StringSerializer();
  private final MockSchemaTransformer transformer = new MockSchemaTransformer();
  private final GenericRecordSerializer serializer =
      new GenericRecordSerializer(transformer, headerSerializer);

  @Parameterized.Parameter(0)
  public String descriptorName;

  @Parameterized.Parameter(1)
  public Message transformed;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    final List<Object[]> parameters = new ArrayList<>();
    final SchemaFactory factory = new SchemaFactory();
    for (final Message message : factory.records()) {
      parameters.add(new Object[] {message.getDescriptorForType().getName(), message});
    }

    return parameters;
  }

  @Test
  public void shouldSchemaDescriptorHeader() {
    // given
    final String topic = "topic";
    final Headers headers = new RecordHeaders();
    transformer.setMessage(transformed);

    // when
    final byte[] serialized = serializer.serialize(topic, headers, new MockRecord());

    // then
    final Header header = headers.lastHeader(GenericRecordDescriptorHeader.DEFAULT_KEY);
    assertThat(header).isNotNull();
    assertThat(header.value()).isEqualTo(headerSerializer.serialize(topic, descriptorName));
    assertThat(serialized).isEqualTo(transformed.toByteArray());
  }
}
