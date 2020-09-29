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
package io.zeebe.exporters.kafka.serde;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Message;
import io.zeebe.exporter.proto.Schema.DeploymentRecord;
import io.zeebe.protocol.immutables.record.ImmutableDeploymentRecordValue;
import io.zeebe.protocol.immutables.record.ImmutableRecord;
import io.zeebe.protocol.record.RecordType;
import io.zeebe.protocol.record.ValueType;
import io.zeebe.protocol.record.intent.DeploymentIntent;
import io.zeebe.protocol.record.intent.Intent;
import java.util.Map;
import org.junit.Test;

public class ProtobufSerdeTest {

  @Test
  public void testProtobufSerde() {

    try (ProtobufRecordSerializer serializer = new ProtobufRecordSerializer()) {
      serializer.configure(Map.of(), false);

      try (ProtobufRecordDeserializer deSerializer = new ProtobufRecordDeserializer()) {
        deSerializer.configure(Map.of(), false);

        final byte[] data =
            serializer.serialize(
                "zeebe",
                ImmutableRecord.builder()
                    .timestamp(1)
                    .key(1)
                    .partitionId(1)
                    .position(1)
                    .intent(
                        Intent.fromProtocolValue(
                            ValueType.DEPLOYMENT, DeploymentIntent.CREATE.getIntent()))
                    .valueType(ValueType.DEPLOYMENT)
                    .recordType(RecordType.COMMAND)
                    .value(ImmutableDeploymentRecordValue.builder().build())
                    .build());

        final Message message = deSerializer.deserialize("zeebe", data);

        // not testing the implementation of RecordTransformer, but just confirming that the
        // Serializer/Deserialize perform the correct steps
        assertThat(message).isInstanceOf(DeploymentRecord.class);
      }
    }
  }
}
