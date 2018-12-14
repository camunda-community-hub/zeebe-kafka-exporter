package io.zeebe.exporter.kafka.configuration;

import io.zeebe.exporter.kafka.RecordSerializer;
import io.zeebe.util.ByteValue;
import io.zeebe.util.DurationUtil;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.record.CompressionType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ProducerConfiguration {
  private String batchSize = "64K";
  private String batchLinger = "100ms";
  private String compressionType = CompressionType.SNAPPY.name;
  private String requestTimeout = "5s";
  private Map<String, String> extra = new HashMap<>();

  public Properties newProperties() {
    final Properties properties = new Properties();

    properties.putAll(extra);
    properties.put(ProducerConfig.BATCH_SIZE_CONFIG, String.valueOf(getBatchSize().toBytes()));
    properties.put(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(getBatchLinger().toMillis()));
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, RecordSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RecordSerializer.class.getName());
    properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, getCompressionType().name);
    properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(getRequestTimeout().toMillis()));

    return properties;
  }

  public ByteValue getBatchSize() {
    return new ByteValue(batchSize);
  }

  public void setBatchSize(String batchSize) {
    this.batchSize = batchSize;
  }

  public Duration getBatchLinger() {
    return DurationUtil.parse(batchLinger);
  }

  public void setBatchLinger(String batchLinger) {
    this.batchLinger = batchLinger;
  }

  public CompressionType getCompressionType() {
    return CompressionType.forName(compressionType);
  }

  public void setCompressionType(String compressionType) {
    this.compressionType = compressionType;
  }

  public Duration getRequestTimeout() {
    return DurationUtil.parse(requestTimeout);
  }

  public void setRequestTimeout(String requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public Map<String, String> getExtra() {
    return extra;
  }

  public void setExtra(Map<String, String> extra) {
    this.extra = extra;
  }

  @Override
  public String toString() {
    return "ProducerConfiguration{" +
      "batchSize='" + batchSize + '\'' +
      ", batchLinger='" + batchLinger + '\'' +
      ", compressionType='" + compressionType + '\'' +
      ", requestTimeout='" + requestTimeout + '\'' +
      ", extra=" + extra +
      '}';
  }
}
