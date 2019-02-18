Zeebe Kafka Exporter
=====================

An easy to use exporter which will export Zeebe records to a configured Kafka topic, compatible with
[zeebe](https://github.com/zeebe-io/zeebe) versions:

- [0.15.0](https://github.com/zeebe-io/zeebe/releases/tag/0.15.0)

If you have any problems please open an issue, write in the forum or on slack.

For more information about the exporters please read the [Exporter documentation](https://docs.zeebe.io/basics/exporters.html).

> This is a work in progress; you're welcome to contribute code or ideas, but no guarantees are made about the exporter itself.
Use at your own risks.

## Installation

The quickest way to get started is:

1. Download the latest release (`zeebe-kafka-exporter-*-uber.jar`).
1. Copy it to the `lib/` folder of your Zeebe brokers.
1. Copy the configuration from `exporter.kafka.cfg.toml` into each broker's `zeebe.cfg.toml` (e.g. append it at the end).
1. Update the configuration's list of servers to point to your Kafka instances.

The next time you start your Zeebe cluster, all event-type records will be exported to their respective Kafka topics.

> There is currently an issue which prevents using this exporter in an isolated way, since it relies on
[zeebe-exporter-protobuf](https://github.com/zeebe-io/zeebe-exporter-protobuf) which is packaged under
`io.zeebe.exporter` (see [zeebe-io/zeebe#2018](https://github.com/zeebe-io/zeebe/issues/2018))

## Usage

The exporter is set up to stream records from Zeebe to Kafka as they are processed by the exporter stream processor.
While this is done asynchronously, to ensure that the position is updated correctly, it keeps buffers in flight requests
and processes their results in the order they were sent, not necessarily in the order the Kafka cluster answered.


Records are serialized to Kafka using
[a common protobuf schema](https://github.com/zeebe-io/zeebe-exporter-protobuf/blob/master/src/main/proto/schema.proto),
where there is one message per record kind (e.g. deployment, job, variable).

The [configuration file](https://github.com/zeebe-io/zeebe-kafka-exporter/blob/master/exporter/exporter.kafka.cfg.toml)
is a good starting point to learn more about how the exporter works.

## Examples

In the [zeebe-kafka-exporter-samples](https://github.com/zeebe-io/zeebe-kafka-exporter/tree/master/samples) module, you
can find examples of different consumers.

### Generic record consumer

Although records are serialized using a different Protobuf message per topic, it is possible to read from multiple
topics by using a `GenericRecordDeserializer`. It relies on the fact that the producer in the exporter uses a
`GenericRecordSerializer` by default, which will serialize a record as a normal `SchemaSerializer<?>` would, but will
additionally write the schema descriptor type in the record headers. This allows the consumer to then deserialize the
message to its correct type, be it `Schema.DeploymentRecord` or `Schema.VariableRecord`.

> This has the unfortunate side effect that you must write code to unpack the message to a concrete type should you need
to; any improvements here would welcome.

An example of a consumer reading from all `zeebe-*` prefixed topics:

```java
final Consumer<Schema.RecordId, GenericRecord> consumer =
    new KafkaConsumer<>(config, new RecordIdDeserializer(), new GenericRecordDeserializer());
consumer.subscribe(Pattern.compile("^zeebe-.*$"));
while (true) {
  final ConsumerRecords<Schema.RecordId, GenericRecord> consumed =
      consumer.poll(Duration.ofSeconds(2));
  for (ConsumerRecord<Schema.RecordId, GenericRecord> record : consumed) {
    logger.info(
        "================[{}] {}-{} ================",
        record.topic(),
        record.key().getPartitionId(),
        record.key().getPosition());
    logger.info("{}", record.value().getMessage().toString());
  }
}
```

### Single topic consumer

Since records are serialized using the same Protobuf message for a single topic, it's possible to consume them and
handle the concrete type directly by using a `SchemaDeserializer<?>` for that type. For example, the following will
consume only workflow instance records, and in the inner loop, the record value is simply the Protobuf message.

```java
final Consumer<Schema.RecordId, Schema.WorkflowInstanceRecord> consumer =
    new KafkaConsumer<>(
        config,
        new RecordIdDeserializer(),
        new SchemaDeserializer<>(Schema.WorkflowInstanceRecord.parser()));
consumer.subscribe(Collections.singleton("zeebe-workflow"));

while (true) {
  final ConsumerRecords<Schema.RecordId, Schema.WorkflowInstanceRecord> consumed =
      consumer.poll(Duration.ofSeconds(2));
  for (ConsumerRecord<Schema.RecordId, Schema.WorkflowInstanceRecord> record : consumed) {
    logger.info(
        "================[{}] {}-{} ================",
        record.topic(),
        record.key().getPartitionId(),
        record.key().getPosition());
    logger.info("{}", record.value().toString());
}
```

## Docker

The [exporter](https://github.com/zeebe-io/zeebe-kafka-exporter/tree/master/exporter) and
[samples](https://github.com/zeebe-io/zeebe-kafka-exporter/tree/master/samples) modules both come with their own
`Dockerfile`; the exporter's will spawn a standard `zeebe` container with a pre-configured exporter, and the samples'
will spawn an OpenJDK container running the `GenericConsumer` example in a loop.

From the root of the project, you can use `docker-compose up -d` to start a `zookeeper`/`kafka` pair (with ports `2181`
and `29092` exposed respectively), a `zeebe` broker/gateway (client port `25600`), and a generic consumer which will
output all records being exported. This is meant primarily to get a feel of how the whole thing works together.

## Reference

The exporter uses a Kafka producer to push records out to different topics based on the incoming record value type (e.g. deployment, raft, etc.)

The producer is configured to be an idempotent producer which will retry a record "forever"; there is a delivery timeout configured, but the timeout is set
to ~25 days, which for most use cases should be enough to fix any recoverable errors. In the case of unrecoverable errors, unfortunately a restart is pretty much
the only solution at the moment, although community contributions are very welcome to fix this.

The main reason records are retried forever is that Zeebe processes records sequentially, and to ensure we've exported a record, we can't update Zeebe and say record 2
has been exported if we can't guarantee that previous records have also been exported (or in Kafka terms, acknowledged).

To take advantage of the asynchronous API and minimize blocking operations, the exporter keeps a queue of in-flight record futures (configurable) and will
export records until that queue is full; once full, it will block until the first element (i.e. the oldest sent record) has been acknowledged by Kafka, at which point
it will then send the next record and resume operation.

At the same time, a background job is scheduled every second to flush the queue of any completed records. So in a best case scenario the queue always has some space and
the exporter never blocks.

## Configuration

A sample configuration file is included in the project under `exporter.kafka.cfg.toml`.

> NOTE: there is currently a bug where the TOML parser used in Zeebe parses all numbers as doubles, which if passed
directly as `ProducerConfig` may cause errors. It's recommended for now to use the extra config arguments for
non-numerial values until that's fixed.

```toml
[[exporters]]
id = "kafka"
className = "io.zeebe.exporters.kafka.KafkaExporter"

  # Top level exporter arguments
  [exporters.args]
  # Controls how many records can have been sent to the Kafka broker without
  # any acknowledgment Once the limit is reached the exporter will block and
  # wait until either one record is acknowledged or awaitInFlightRecordTimeout
  # time is elapsed, at which point an error is produced and the record is
  # retried at a later time
  maxInFlightRecords = 1000
  # Controls how long we should block and await a record to be acknowledged
  # by the Kafka broker when the maximum number of in-flight records is reached
  # Format is the same as standard Zeebe configuration timeouts/durations
  awaitInFlightRecordTimeout = "5s"

  # Producer specific configuration
  [exporters.args.producer]
  # The list of initial Kafka broker contact points. The format should be the same
  # one as the ProducerConfig expects, i.e. "host:port"
  # Maps to ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
  servers = [ "kafka:9092" ]
  # Controls how long the producer will wait for a request to be acknowledged by
  # the Kafka broker before retrying it
  # Maps to ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG
  # Format is the same as standard Zeebe configuration timeouts/durations
  requestTimeout = "5s"

  # Any setting under the following section will be passed verbatim to
  # ProducerConfig; you can use this to configure authentication, compression,
  # etc. Note that you can overwrite some important settings, so avoid changing
  # idempotency, delivery timeout, and retries, unless you know what you're doing
  [exporters.args.producer.config]

  # Controls which records are pushed to Kafka and to which topic
  # Each entry is a sub-map which can contain two entries:
  #     type => [string]
  #     topic => string
  #
  # Topic is the topic to which the record with the given value type
  # should be sent to, e.g. for a deployment record below we would
  # send the record to "zeebe-deployment" topic.
  #
  # Type is a list of accepted record types, allowing you to filter
  # if you want nothing ([]), commands (["command"]), events (["events"]),
  # or rejections (["rejection"]), or a combination of the three, e.g.
  # ["command", "event"].
  [exporters.args.records]
  # If a record value type is omitted in your configuration file,
  # it will fall back to whatever is configured in the defaults
  defaults = { type = [ "event" ], topic = "zeebe" }
  # For records with a value of type DEPLOYMENT
  deployment = { topic = "zeebe-deployment" }
  # For records with a value of type INCIDENT
  incident = { topic = "zeebe-incident" }
  # For records with a value of type JOB_BATCH
  jobBatch = { topic = "zeebe-job-batch" }
  # For records with a value of type JOB
  job = { topic = "zeebe-job" }
  # For records with a value of type MESSAGE
  message = { topic = "zeebe-message" }
  # For records with a value of type MESSAGE_SUBSCRIPTION
  messageSubscription = { topic = "zeebe-message-subscription" }
  # For records with a value of type MESSAGE_START_EVENT_SUBSCRIPTION
  messageStartEventSubscription = { topic = "zeebe-message-subscription-start-event" }
  # For records with a value of type RAFT
  raft = { topic = "zeebe-raft" }
  # For records with a value of type TIMER
  timer = { topic = "zeebe-timer" }
  # For records with a value of type VARIABLE
  variable = { topic = "zeebe-variable" }
  # For records with a value of type WORKFLOW_INSTANCE
  workflowInstance = { topic = "zeebe-workflow" }
  # For records with a value of type WORKFLOW_INSTANCE_SUBSCRIPTION
  workflowInstanceSubscription = { topic = "zeebe-workflow-subscription" }
```
