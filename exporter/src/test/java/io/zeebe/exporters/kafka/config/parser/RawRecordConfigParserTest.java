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
package io.zeebe.exporters.kafka.config.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zeebe.exporters.kafka.config.RecordConfig;
import io.zeebe.exporters.kafka.config.raw.RawRecordConfig;
import io.zeebe.protocol.record.RecordType;
import java.util.EnumSet;
import org.junit.Test;

public class RawRecordConfigParserTest {
  private final RawRecordConfigParser parser = new RawRecordConfigParser();

  @Test
  public void shouldParseAllowedTypes() {
    // given
    final RawRecordConfig config = new RawRecordConfig();
    config.type =
        String.format("%s,%s", AllowedType.COMMAND.getTypeName(), AllowedType.EVENT.getTypeName());

    // when
    final RecordConfig parsed = parser.parse(config);

    // then
    assertThat(parsed.getAllowedTypes())
        .containsExactlyInAnyOrder(RecordType.COMMAND, RecordType.EVENT);
  }

  @Test
  public void shouldParseTopic() {
    // given
    final RawRecordConfig config = new RawRecordConfig();
    config.topic = "something";

    // when
    final RecordConfig parsed = parser.parse(config);

    // then
    assertThat(parsed.getTopic()).isEqualTo("something");
  }

  @Test
  public void shouldSetDefaultsIfNull() {
    // given
    final RawRecordConfig config = new RawRecordConfig();

    // when
    final RecordConfig parsed = parser.parse(config);

    // then
    assertThat(parsed.getTopic()).isEqualTo(RawRecordConfigParser.DEFAULT_TOPIC_NAME);
    assertThat(parsed.getAllowedTypes()).isEqualTo(RawRecordConfigParser.DEFAULT_ALLOWED_TYPES);
  }

  @Test
  public void shouldSetExplicitDefaultsIfNull() {
    // given
    final RecordConfig defaults = new RecordConfig(EnumSet.allOf(RecordType.class), "topic");
    final RawRecordConfigParser explicitParser = new RawRecordConfigParser(defaults);
    final RawRecordConfig config = new RawRecordConfig();

    // when
    final RecordConfig parsed = explicitParser.parse(config);

    // then
    assertThat(parsed.getTopic()).isEqualTo(defaults.getTopic());
    assertThat(parsed.getAllowedTypes()).isEqualTo(defaults.getAllowedTypes());
  }

  @Test
  public void shouldThrowExceptionIfAllowedTypeIsUnknown() {
    // given
    final RawRecordConfig config = new RawRecordConfig();
    config.type = "something unlikely";

    // when - then
    assertThatThrownBy(() -> parser.parse(config)).isInstanceOf(IllegalArgumentException.class);
  }
}
