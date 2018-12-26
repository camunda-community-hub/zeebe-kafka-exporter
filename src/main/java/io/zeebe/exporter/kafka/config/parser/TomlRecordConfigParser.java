package io.zeebe.exporter.kafka.config.parser;

import io.zeebe.exporter.kafka.config.RecordConfig;
import io.zeebe.exporter.kafka.config.toml.TomlRecordConfig;
import io.zeebe.exporter.kafka.record.AllowedType;
import io.zeebe.protocol.clientapi.RecordType;

import java.util.EnumSet;

public class TomlRecordConfigParser implements Parser<TomlRecordConfig, RecordConfig> {
  @Override
  public RecordConfig parse(TomlRecordConfig config) {
    final RecordConfig parsed = new RecordConfig();

    if (config.type != null) {
      parsed.allowedTypes = EnumSet.noneOf(RecordType.class);
      config.type.forEach(t -> parsed.allowedTypes.add(AllowedType.forName(t).recordType));
    }

    if (config.topic != null) {
      parsed.topic = config.topic;
    }

    return parsed;
  }
}
