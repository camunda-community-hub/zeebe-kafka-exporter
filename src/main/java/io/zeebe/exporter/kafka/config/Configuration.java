package io.zeebe.exporter.kafka.config;

public class Configuration {
  public String topic;
  public int maxInFlightRecords = 1_000;
  public String awaitInFlightRecordTimeout = "5s";
  public ProducerConfiguration producer = new ProducerConfiguration();
  public RecordsConfiguration records = new RecordsConfiguration();
}
