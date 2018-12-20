package io.zeebe.exporter.kafka.config;

public class RecordsConfiguration {
  public RecordConfiguration deployment;
  public RecordConfiguration incident;
  public RecordConfiguration jobBatch;
  public RecordConfiguration job;
  public RecordConfiguration message;
  public RecordConfiguration messageSubscription;
  public RecordConfiguration raft;
  public RecordConfiguration timer;
  public RecordConfiguration workflowInstance;
  public RecordConfiguration workflowInstanceSubscription;
}
