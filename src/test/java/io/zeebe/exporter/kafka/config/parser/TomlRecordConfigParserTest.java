package io.zeebe.exporter.kafka.config.parser;

import io.zeebe.exporter.kafka.config.RecordConfig;
import io.zeebe.exporter.kafka.config.toml.TomlRecordConfig;
import io.zeebe.exporter.kafka.record.AllowedType;
import io.zeebe.protocol.clientapi.RecordType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TomlRecordConfigParserTest {
  private final TomlRecordConfigParser parser = new TomlRecordConfigParser();

  @Test
  public void shouldParseAllowedTypes() {
    // given
    final TomlRecordConfig config = new TomlRecordConfig();
    config.type = Arrays.asList(AllowedType.COMMAND.name, AllowedType.EVENT.name);

    // when
    final RecordConfig parsed = parser.parse(config);

    // then
    assertThat(parsed.allowedTypes).containsExactlyInAnyOrder(RecordType.COMMAND, RecordType.EVENT);
  }

  @Test
  public void shouldParseTopic() {
    // given
    final TomlRecordConfig config = new TomlRecordConfig();
    config.topic = "something";

    // when
    final RecordConfig parsed = parser.parse(config);

    // then
    assertThat(parsed.topic).isEqualTo("something");
  }

  @Test
  public void shouldNotSetAnythingIfNull() {
    // given
    final TomlRecordConfig config = new TomlRecordConfig();

    // when
    final RecordConfig parsed = parser.parse(config);

    // then
    assertThat(parsed.topic).isNull();
    assertThat(parsed.allowedTypes).isNull();
  }

  @Test
  public void shouldThrowExceptionIfAllowedTypeIsUnknown() {
    // given
    final TomlRecordConfig config = new TomlRecordConfig();
    config.type = Collections.singletonList("something unlikely");

    // when - then
    assertThatThrownBy(() -> parser.parse(config)).isInstanceOf(IllegalArgumentException.class);
  }
}
