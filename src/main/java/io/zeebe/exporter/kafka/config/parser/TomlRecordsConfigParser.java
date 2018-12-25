package io.zeebe.exporter.kafka.config.parser;

import io.zeebe.exporter.kafka.config.Parser;
import io.zeebe.exporter.kafka.config.RecordConfig;
import io.zeebe.exporter.kafka.config.RecordsConfig;
import io.zeebe.exporter.kafka.config.toml.TomlRecordConfig;
import io.zeebe.exporter.kafka.config.toml.TomlRecordsConfig;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;

import java.util.EnumSet;

public class TomlRecordsConfigParser implements Parser<TomlRecordsConfig, RecordsConfig> {
  public static final String DEFAULT_TOPIC_NAME = "zeebe";
  public static final EnumSet<RecordType> DEFAULT_ALLOWED_TYPES = EnumSet.allOf(RecordType.class);

  private final Parser<TomlRecordConfig, RecordConfig> recordConfigParser;

  public TomlRecordsConfigParser() {
    this.recordConfigParser = new TomlRecordConfigParser();
  }

  public TomlRecordsConfigParser(Parser<TomlRecordConfig, RecordConfig> recordConfigParser) {
    this.recordConfigParser = recordConfigParser;
  }

  @Override
  public RecordsConfig parse(TomlRecordsConfig config) {
    final RecordsConfig parsed = new RecordsConfig();

    parsed.defaults = recordConfigParser.parse(config.defaults, RecordConfig::new);

    if (parsed.defaults.topic == null) {
      parsed.defaults.topic = DEFAULT_TOPIC_NAME;
    }

    if (parsed.defaults.allowedTypes == null) {
      parsed.defaults.allowedTypes = DEFAULT_ALLOWED_TYPES;
    }

    parsed.typeMap.put(ValueType.DEPLOYMENT, parseOrDefault(parsed, config.deployment));
    parsed.typeMap.put(ValueType.INCIDENT, parseOrDefault(parsed, config.incident));
    parsed.typeMap.put(ValueType.JOB, parseOrDefault(parsed, config.job));
    parsed.typeMap.put(ValueType.JOB_BATCH, parseOrDefault(parsed, config.jobBatch));
    parsed.typeMap.put(ValueType.MESSAGE, parseOrDefault(parsed, config.message));
    parsed.typeMap.put(ValueType.MESSAGE_SUBSCRIPTION, parseOrDefault(parsed, config.messageSubscription));
    parsed.typeMap.put(ValueType.RAFT, parseOrDefault(parsed, config.raft));
    parsed.typeMap.put(ValueType.TIMER, parseOrDefault(parsed, config.timer));
    parsed.typeMap.put(ValueType.WORKFLOW_INSTANCE, parseOrDefault(parsed, config.workflowInstance));
    parsed.typeMap.put(ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION, parseOrDefault(parsed, config.workflowInstanceSubscription));

    return parsed;
  }

  private RecordConfig parseOrDefault(RecordsConfig recordsConfig, TomlRecordConfig config) {
    final RecordConfig parsed = recordConfigParser.parse(config, RecordConfig::new);

    if (parsed.topic == null) {
      parsed.topic = recordsConfig.defaults.topic;
    }

    if (parsed.allowedTypes == null) {
      parsed.allowedTypes = recordsConfig.defaults.allowedTypes;
    }

    return parsed;
  }
}
