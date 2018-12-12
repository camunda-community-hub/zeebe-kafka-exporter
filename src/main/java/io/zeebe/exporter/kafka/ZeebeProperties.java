package io.zeebe.exporter.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class ZeebeProperties extends Properties {
  private final Configuration configuration;

  public ZeebeProperties(Configuration configuration) {
    this.configuration = configuration;

    setRequiredProperties();
    setBatching();
    setCompression();
    setSerializers();
    setTimeouts();
    setExtraProperties();
  }

  protected void setExtraProperties() {
    putAll(configuration.getExtraProperties());
  }

  protected void setRequiredProperties() {
    // Sets the known servers, a list of host:port pair, e.g. "localhost:3000,localhost:5000"
    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getServers());
    // Sets the current topic
  }

  protected void setSerializers() {
    // Use RecordKeySerializer as key serializer
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, RecordKeySerializer.class);
    // Use RecordSerializer as value serializer
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RecordSerializer.class);
  }

  protected void setBatching() {
    // Batch up to 64K buffer sizes
    put(ProducerConfig.BATCH_SIZE_CONFIG, configuration.getBatchSize().toBytes());
    // Linger up to 100 ms before sending batch if size not met
    put(ProducerConfig.LINGER_MS_CONFIG, configuration.getBatchLinger().toMillis());
  }

  protected void setCompression() {
    // Use snappy for compression
    put(ProducerConfig.COMPRESSION_TYPE_CONFIG, configuration.getCompressionType().toString());
  }

  protected void setTimeouts() {
    // Set a 5 second request timeout
    put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, configuration.getRequestTimeout().toMillis());
  }
}
