package io.zeebe.exporter.kafka;

public class KafkaExporterException extends RuntimeException {
  public KafkaExporterException(String message) {
    super(message);
  }

  public KafkaExporterException(String message, Throwable cause) {
    super(message, cause);
  }
}
