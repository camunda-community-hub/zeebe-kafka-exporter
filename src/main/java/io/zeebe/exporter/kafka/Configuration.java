package io.zeebe.exporter.kafka;

import io.zeebe.util.ByteValue;
import io.zeebe.util.DurationUtil;
import org.apache.kafka.common.record.CompressionType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
  private List<String> servers = new ArrayList<>();
  private Duration requestTimeout = Duration.ofSeconds(5);
  private ByteValue batchSize = ByteValue.ofKilobytes(16);
  private Duration batchLinger = Duration.ofSeconds(5);
  private CompressionType compressionType = CompressionType.SNAPPY;

  private Map<String, ?> extraProperties = new HashMap<>();

  public Map<String, ?> getExtraProperties() {
    return extraProperties;
  }

  public void setExtraProperties(Map<String, ?> extraProperties) {
    this.extraProperties = extraProperties;
  }

  public ByteValue getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(ByteValue batchSize) {
    this.batchSize = batchSize;
  }

  public void setBatchSize(String batchSize) {
    this.batchSize = new ByteValue(batchSize);
  }

  public void setBatchSize(long batchSize) {
    this.batchSize = ByteValue.ofBytes(batchSize);
  }

  public Duration getBatchLinger() {
    return batchLinger;
  }

  public void setBatchLinger(Duration batchLinger) {
    this.batchLinger = batchLinger;
  }

  public void setBatchLinger(String batchLinger) {
    this.batchLinger = DurationUtil.parse(batchLinger);
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public void setRequestTimeout(String requestTimeout) {
    this.requestTimeout = DurationUtil.parse(requestTimeout);
  }

  public CompressionType getCompressionType() {
    return compressionType;
  }

  public void setCompressionType(CompressionType compressionType) {
    this.compressionType = compressionType;
  }

  public void setCompressionType(String name) {
    this.compressionType = CompressionType.forName(name);
  }

  public void setCompressionType(int id) {
    this.compressionType = CompressionType.forId(id);
  }

  public List<String> getServers() {
    return servers;
  }

  public void setServers(List<String> servers) {
    this.servers = servers;
  }
}
